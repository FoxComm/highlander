
import React from 'react';
import styles from './app.css';
import cssModules from 'react-css-modules';

@cssModules(styles)
export default class App extends React.Component {

  render() {
    return (
      <main styleName="main">
        <h2>Welcome to this awesome storefront.</h2>
      </main>
    );
  }
}

export default App;
