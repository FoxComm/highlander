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

  console.log("Transition state: " + state + " => " + trans.state);

  return trans;
}

async function simulate(persona) {

  var context = {
    page: Nightmare({
        webPreferences: {
          partition: "stimulator-"+Math.random()
    }}),
    persona: persona,
    state: persona.start,
    args: {}
  };

  while(context.state) {
    try {

      //process state
      var f = stateFunctions[context.state];

      if(_.isNil(f)) {
        throw "State " + context.state + " is undefined";
      }
      await f(context);

      //transition
      var trans = transition(context.state, context.persona.states[context.state]);

      context.state = trans.state;
      context.args = trans.args;

    } catch (e) {
      console.error(e);
    }
  }

  context.page.end();

  console.log("Simulation Complete: " + persona.name);
}

simulate(personas[0]);
