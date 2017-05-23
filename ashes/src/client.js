import { render } from 'react-dom';
import App from './app';

render(
  App,
  document.getElementById('foxcom')
);

// See https://github.com/gaearon/react-hot-loader/issues/565
if (module.hot) {
  module.hot.accept();

  // Uncommenting this will cause full page reload on change

  // module.hot.accept(['./root', './routes', './store'], () => {
  //   try {
  //     require('./root');
  //     require('./routes');
  //   } catch (e) {
  //     // pass
  //   }
  //   // do nothing, only css reload works
  //   // because of, for example, https://github.com/pauldijou/redux-act/issues/42
  // });
}
