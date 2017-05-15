#### Basic usage

```javascript
<PageNav>
  <IndexLink to="orders">Orders</IndexLink>
  <Link to="carts">Carts</Link>
</PageNav>
```

```
const useNamedRoutes = require('use-named-routes');
const { createHashHistory } = require('history');
const { Router, Route, useRouterHistory } = require('react-router');
const { Link, IndexLink } = require('../../link');

const createBrowserHistory = useNamedRoutes(useRouterHistory(createHashHistory));

const style = { textAlign: 'center', marginBottom: '40px', textTransform: 'uppercase' };

const Page = (props) => <div style={style}>{props.route.name} page</div>;

const App = (props) => (
  <div>
    <PageNav>
      <Link to="styles">Styles</Link>
      <Link to="flow">Flow</Link>
    </PageNav>
    {props.children}
  </div>
);

const routes = (
  <Route path="/" component={App}>
    <Route path="/styles" name="styles" component={Page} />
    <Route path="/flow" name="flow" component={Page} />
    <Route path="*" name="not-found" component={Page} />
  </Route>
);

const AppExtended = (props) => (
  <div>
    <PageNav>
      <Link to="documentation">Documentation</Link>
      <Link to="components">Components</Link>
      <Link to="pagenav">PageNav</Link>
      <Link to="button">Button</Link>
      <Link to="buttonwithmenu">ButtonWithMenu</Link>
      <Link to="saveacncel">SaveCancel</Link>
      <Link to="swatchinput">SwatchInput</Link>
      <Link to="textmask">TextMask</Link>
    </PageNav>
    {props.children}
  </div>
);

const routesExtended = (
  <Route path="/" component={AppExtended}>
    <Route path="/documentation" name="documentation" component={Page} />
    <Route path="/components" name="components" component={Page} />
    <Route path="/pagenav" name="pagenav" component={Page} />
    <Route path="/button" name="button" component={Page} />
    <Route path="/buttonwithmenu" name="buttonwithmenu" component={Page} />
    <Route path="/saveacncel" name="saveacncel" component={Page} />
    <Route path="/swatchinput" name="swatchinput" component={Page} />
    <Route path="/textmask" name="textmask" component={Page} />
    <Route path="*" name="not-found" component={Page} />
  </Route>
);

<div>
  <Router history={createBrowserHistory({ routes })}>
    {routes}
  </Router>

  <Router history={createBrowserHistory({ routes: routesExtended })}>
    {routesExtended}
  </Router>
</div>
```
