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

const testFacets = [
  {
    name: 'Gender',
    kind: 'checkbox',
    values: [
      {label: 'Men', value: 'men', click: null},
      {label: 'Women', value: 'women', click: null},
      {label: 'Kids', value: 'kids', click: null},
    ]
  },
  {
    name: 'Shoes',
    kind: 'checkbox',
    values: [
      {label: 'Shoes', value: 'shoes', click: null},
      {label: 'High Tops', value: 'hightops', click: null},
    ]
  },
  {
    name: 'Size',
    kind: 'circle',
    values: [
      {label: '4', value: '4', click: null},
      {label: '4.5', value: '4.5', click: null},
      {label: '5', value: '5', click: null},
      {label: '5.5', value: '5.5', click: null},
      {label: '9', value: '9', click: null},
      {label: '9.5', value: '9.5', click: null},
      {label: '10', value: '10', click: null},
      {label: '10.5', value: '10.5', click: null},
      {label: '14', value: '14', click: null},
      {label: '14.5', value: '14.5', click: null},
    ]
  },
  {
    name: 'Franchise',
    kind: 'checkbox',
    values: [
      {label: 'Cloudform', value: 'cloudform', click: null},
      {label: 'Raleigh', value: 'raleigh', click: null},
      {label: 'Avantage Clean', value: 'avantageclean', click: null},
      {label: 'Life Racer', value: 'liferacer', click: null},
      {label: 'BB9TIS', value: 'bb8tis', click: null},
    ]
  },
  {
    name: 'Colors',
    kind: 'color',
    values: [
      {label: 'red', value: 'red', click: null},
      {label: 'green', value: 'green', click: null},
      {label: 'blue', value: 'blue', click: null},
      {label: 'white', value: 'white', click: null},
      {label: 'yellow', value: 'yellow', click: null},
      {label: 'floralwhite', value: 'floralwhite', click: null},
    ]
  },

]

class Facets extends Component {
  props: FacetsProps;
  state: FacetsState = {};

  static defaultProps = {};

  @autobind
  handleClickFacets(facet): void {
    if (this.props.onSelect) {
      this.props.onSelect(facet);
    }
  }

  @autobind
  renderFacet(f) { 
    let values = {};

    if(f.kind == 'checkbox') {
      values = _.map(f.values, (v) => {
        return (<Checkbox 
          value={v.value} 
          label={v.label} 
          checked={false}
          click={this.handleClickFacets}
          />);
      });
    } else if(f.kind == 'circle') {
      values = _.map(f.values, (v) => {
        return (<Circle 
          value={v.value} 
          label={v.label} 
          checked={false}
          click={this.handleClickFacets}
          />);
      });
    } else if(f.kind == 'color') {
      values = _.map(f.values, (v) => {
        return (<ColorCircle 
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
    const { t } = this.props;

    //const facets = _.map(this.props.facets, (f) => {
    const facets = _.map(testFacets, (f) => {
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
