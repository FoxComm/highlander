import React, { Component } from 'react';
import Home from './Home';
import Api from './Api';
import {
  BrowserRouter as Router,
  Route,
} from 'react-router-dom';

class App extends Component {
  render() {
    return (
      <Router>
        <div>
          <Route exact path="/" component={Home} />
          <Route path="/api" component={Api} />
        </div>
      </Router>
    );
  }
}

export default App;
