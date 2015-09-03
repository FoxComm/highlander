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
      breadcrumb = (
        <div className="breadcrumb">
          {modelName} <i className="fa fa-chevron-right"></i> {params[model]}
        </div>
      );
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
            <i className="fa fa-bell"></i>
          </div>
          <div className="sort">Name <i className="fa fa-chevron-down"></i></div>
        </div>
      </header>
    );
  }
}

Header.contextTypes = {
  router: React.PropTypes.func
};
