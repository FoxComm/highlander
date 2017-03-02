#SRC

The simulation code is composed of several orthogonal pieces. The types of users
are defined in [personas.json](personas.json). The definition is a state machine
with transition probabilities and parameters for each state.

The functions for each state are defined in several simulators. For example,
the [night_simulator.js](night_simulator.js) implements a nightmare.js version
of the simulator while the [api_simulator.js](api_simulator.js) implements an
api.js based simulator.

There is also an empty [dummy_simulator.js](dummy_simulator.js) that does nothing
and provides a good start for creating another simulator.

[simulate.js](simulate.js) is the main driver application and accepts several
parameters, including the persona and simulator to use. It processes each state
from the persona transitioning between states based on the state transitions defined
in the persona. The process runs forever until a null state is reached.


