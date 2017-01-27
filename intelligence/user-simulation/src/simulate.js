const _ = require('lodash');
const { faker}  = require('faker');
const Nightmare = require('nightmare');
const personas  = require('./personas.json');
const {stateFunctions} = require('./states.js');

var home = "https://hal.foxcommerce.com";

function transition(state, states) {
  var trans = null;
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

async function simulate(persona) {

  var context = {
    page: Nightmare({
        show: false,
        webPreferences: {
          partition: 'nopersist'
    }}),
    persona: persona,
    state: persona.start,
    args: {}
  };

  while(context.state) {
      //process state
      console.log(context.state);

      var f = stateFunctions[context.state];

      if(_.isNil(f)) {
        throw "State " + context.state + " is undefined";
      }
      await f(context);

      //transition
      var trans = transition(context.state, context.persona.states[context.state]);

      context.state = trans.state;
      context.args = trans.args;
  }

  context.page.end();

  console.log("Simulation Complete: " + persona.name);
}


var personaName = process.argv[2];
var persona = personas[personaName];

if(_.isNil(persona)) {
  console.log("'" + personaName + "' is not a valid persona, choose one of the following...");
  var personaNames = _.map(personas, (v, key) => { return key});
  console.log(personaNames);
  process.exit(0);
}

console.log("Processing persona: " + personaName);
simulate(persona);
