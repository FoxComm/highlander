'use strict';

import React from 'react';
import { inflect } from 'fleck';

export default class Header extends React.Component {
  render() {
    let
      { router }  = this.context,
      params      = router.getCurrentParams(),
      model       = Object.keys(params)[0],
      breadcrumb  = null;

    if (model) {
      let modelName = inflect(model, 'pluralize', 'capitalize');
      breadcrumb = <div className="breadcrumb">{modelName} <i className="icon-right-open"></i> {params[model]}</div>;
    } else {
      let modelName = router.getCurrentPathname();
      modelName = modelName.replace(/^\//, '');
      modelName = inflect(modelName, 'pluralize', 'capitalize');
      breadcrumb = <div className="breadcrumb">{modelName}</div>;
    }

    return (
      <header role='banner'>
        {breadcrumb}
        <div className="sub-nav">
          <div className="notifications">
            <i className="icon-bell-alt"></i>
          </div>
          <div className="sort">Name <i className="icon-down-open"></i></div>
        </div>
      </header>
    );
  }
}

Header.contextTypes = {
  router: React.PropTypes.func
};
