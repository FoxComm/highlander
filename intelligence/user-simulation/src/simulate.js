const _ = require('lodash');
const { faker}  = require('faker');
const Nightmare = require('nightmare');
const personas  = require('./personas.json');
const nightSimulator = require('./night_states.js');
const dummySimulator = require('./dummy_states.js');
const apiSimulator = require('./api_states.js');

/** 
 * The simulation processes a state machine until the terminal state is reached.
 * The context contains a persona which specifies the states and their transitions.
 * Each transition has a probability and some parameters
 *
 * The stateFunctions is a map of states to functions that execute them.
 * They take the context as a parameter.
 *
 * After the state function is called we execute a state transition which 
 * Uses the probabilities in the persona to decide which state to take.
 */


/**
 * The state transition function returns a transition object based on the 
 * probabilities defined in the persona. The transition object contains the 
 * state and the arguments for that state. The arguments are set in the context 
 * and will be available in the state function.
 */
function transition(state, states) {
  let trans = null;
  if (_.size(states) == 0) {
    return null;
  }

  while(_.isNil(trans)) {
    _.each(states, (val, s) => {
      const r = Math.random();
      if(r <= val.p) {
        trans = {
          state: s,
          args: val
        };
      }
    });
  }

  return trans;
}

/**
 * Simulation function executes until a terminal state is reached.
 */
async function simulate(context, stateFunctions) {

  let procId = Math.floor(Math.random() * 1000);

  while(context.state) {
    try {
      //process state
      console.log(procId + ": " + context.state);

      let f = stateFunctions[context.state];

      if(_.isNil(f)) {
        throw "State " + context.state + " is undefined";
      }
      await f(context);

      //transition
      let trans = transition(context.state, context.persona.states[context.state]);

      context.state = trans.state;
      context.args = trans.args;
    } catch(e) {
      console.error(e);
      process.exit(1);
    }
  }

  context.page.end();

  console.log("Simulation Complete: " + persona.name);
}


let simName = process.argv[2];
let personaName = process.argv[3];

/*************************************
 * get simulator functions
 *************************************/
let simulator = dummySimulator

switch(simName) {
  case "dummy" : simulator = dummySimulator; break;
  case "api" : simulator = apiSimulator; break;
  case "night" : simulator = nightSimulator; break;
  default: {
      console.log("Simulator " + simName + " is unknown, must be dummy, api, or night");
      process.exit(1);
  }
}

/*************************************
 * get persona
 *************************************/
let persona = personas[personaName];

if(_.isNil(persona)) {
  console.log("'" + personaName + "' is not a valid persona, choose one of the following...");
  let personaNames = _.map(personas, (v, key) => { return key});
  console.log(personaNames);
  process.exit(1);
}

let home = "https://hal.foxcommerce.com";


/*************************************
 * setup simulation context
 *************************************/
let stripeKey = process.env.STRIPE_PUBLISHABLE_KEY; 
let context = {
  home: home,
  persona: persona,
  state: persona.start,
  args: {},
  stripeKey: stripeKey
};

simulator.setup(context);

/*************************************
 *do it, pick up your boots and do it!
 *************************************/
simulate(context, simulator.stateFunctions);
