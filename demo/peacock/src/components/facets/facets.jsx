/* flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import { browserHistory } from 'lib/history';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import styles from './facets.css';
import Checkbox from './kind/checkbox';
import Circle from './kind/circle';
import ColorCircle from './kind/colorcircle';
import _ from 'lodash';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

type FacetsProps = {
  facets: Array<Object>,
  onSelect: ?Function,
};

type FacetsState = {
};

class Facets extends Component {
  props: FacetsProps;
  state: FacetsState = {};

  static defaultProps = {
    facets: [],
  };

  @autobind
  handleClickFacets(facet, value, selected): void {
    if (this.props.onSelect) {
      this.props.onSelect(facet, value, selected);
    }
  }

  @autobind
  renderFacet(f) { 
    let values = {};

    if(f.kind == 'checkbox') {
      values = _.map(f.values, (v) => {
        return (<Checkbox 
          facet={f.key}
          value={v.value} 
          label={v.label} 
          checked={false}
          click={this.handleClickFacets}
          />);
      });
    } else if(f.kind == 'circle') {
      values = _.map(f.values, (v) => {
        return (<Circle 
          facet={f.key}
          value={v.value} 
          label={v.label} 
          checked={false}
          click={this.handleClickFacets}
          />);
      });
    } else if(f.kind == 'color') {
      values = _.map(f.values, (v) => {
        return (<ColorCircle 
          facet={f.key}
          value={v.value} 
          label={v.label} 
          checked={false}
          click={this.handleClickFacets}
          />);
      });
    } else {
      values = (<div> 'unsuported type' </div>);
    }
    let facetStyle = f.kind + '-facet';
    return (
      <div styleName = 'facet'>
        <h2>{f.name}</h2>
        <div styleName={facetStyle}>
        {values}
        </div>
      </div>
    );
  }

  render(): HTMLElement {
    //const facets = _.map(this.props.facets, (f) => {
    const facets = _.map(this.props.facets, (f) => {
      return this.renderFacet(f);
    });

    return (
      <div styleName="facets">
        {facets}
      </div>
    );
  }
}


export default Facets;
