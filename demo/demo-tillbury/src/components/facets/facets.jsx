/* flow */

import _ from 'lodash';
import classnames from 'classnames';
import { assoc } from 'sprout-data';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import styles from './facets.css';

// components
import Checkbox from './kind/checkbox';
import Circle from './kind/circle';
import ColorCircle from './kind/colorcircle';
import Image from './kind/image';
import Accordion from 'ui/accordion/accordion';
import SelectBox from './renderer/selectbox';

import type { Facet as TFacet, FacetValue } from 'types/facets';

type FacetsProps = {
  className?: string,
  isPdp?: boolean,
  prefix: string,
  facets: Array<TFacet>,
  whitelist?: Array<string>,
  onSelect?: (facet: string, value: string, selected: boolean) => void,
};

type State = {
  animationState: {[key: string]: boolean},
  facetMessages: {[key: string]: string|null},
}

class Facets extends Component {
  props: FacetsProps;

  state: State = {
    animationState: {},
    facetMessages: {},
  };

  static defaultProps = {
    facets: [],
    whitelist: [],
    prefix: '',
  };

  @autobind
  handleClickFacets(facet: string, value: string, selected: boolean): void {
    const { facetMessages } = this.state;
    this.setState({
      facetMessages: assoc(facetMessages, facet, null),
    });
    if (this.props.onSelect) {
      this.props.onSelect(facet, value, selected);
    }
  }

  @autobind
  renderValues(f: TFacet) {
    const { prefix } = this.props;

    if (f.kind == 'select') {
      return <SelectBox facet={f} onChange={this.handleClickFacets} />;
    }

    return _.map(f.values, (v: FacetValue) => {
      const key = `val-${prefix}-${f.kind}-${f.key}-${v.label}`;
      let w = null;

      if (f.kind == 'checkbox') {
        w = (<Checkbox
          key={key}
          reactKey={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={v.selected}
          click={this.handleClickFacets}
        />);
      } else if (f.kind == 'circle') {
        w = (<Circle
          key={key}
          reactKey={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={v.selected}
          click={this.handleClickFacets}
        />);
      } else if (f.kind == 'color') {
        w = (<ColorCircle
          key={key}
          reactKey={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={v.selected}
          click={this.handleClickFacets}
        />);
      } else if (f.kind == 'image') {
        w = (<Image
          key={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={v.selected}
          click={this.handleClickFacets}
        />);
      }
      return w;
    });
  }

  flashUnselectedFacets(facets: Array<TFacet>) {
    const animationState = {};
    const facetMessages = {};
    _.forEach(facets, (facet: TFacet) => {
      animationState[facet.key] = true;
      facetMessages[facet.key] = `We need to know your ${facet.name.toLowerCase()} first!`;
    });
    this.setState({
      animationState,
      facetMessages,
    });
  }

  resetFacetAnimation(key: string) {
    const newState = assoc(this.state.animationState, key, false);
    this.setState({
      animationState: newState,
    });
  }

  renderNormalFacet(f: TFacet) {
    const values = this.renderValues(f);
    const facetStyle = `${f.kind}-facet`;

    const className = classnames(styles.facet, {
      [styles['facet-flash']]: this.state.animationState[f.key],
    });
    let message = this.state.facetMessages[f.key];
    let facetValue = null;

    // show facet value only for color kind
    if (f.key === 'color') {
      const selectedValue: ?FacetValue = _.find(f.values, (v: FacetValue) => v.selected);
      if (selectedValue && selectedValue.label) {
        facetValue = selectedValue.label;
        message = null;
      }
    }

    return (
      <div key={f.key} className={className} onAnimationEnd={() => this.resetFacetAnimation(f.key)}>
        <div styleName="facet-header">
          <div styleName="facet-name">{f.name}{facetValue ? ':' : null}</div>
          {message && <div styleName="facet-message">{message}</div>}
          {facetValue && <div styleName="facet-value">{facetValue}</div>}
        </div>
        <div styleName={facetStyle}>
          {values}
        </div>
      </div>
    );
  }

  renderAccordionFacet(f: TFacet) {
    const values = this.renderValues(f);
    const facetStyle = `${f.kind}-facet`;
    const contentClass = classnames(styles[facetStyle], 'acc-content');

    return (
      <Accordion title={f.name} key={f.key}>
        <div className={contentClass}>
          {values}
        </div>
      </Accordion>
    );
  }

  render(): Element<*> {
    const { facets, whitelist, className } = this.props;

    // Only show facets that are in the white list if a white list is specified,
    // or that have more than one element.
    // Only showing facets with more then one element was decided because it
    // matches the expected behaviour based on research of other sites.
    const renderable = _.filter(facets, (f) => {
      return f.values.length > 1
        && (_.isEmpty(whitelist) || _.find(whitelist, key => _.toLower(key) == _.toLower(f.key)));
    });

    const renderFacet = this.props.isPdp ? this.renderNormalFacet : this.renderAccordionFacet;

    const rendered = _.map(renderable, (f: TFacet) => {
      return renderFacet.call(this, f);
    });

    if (rendered.length == 0) return null;

    return (
      <div styleName="facets" className={className}>
        {rendered}
      </div>
    );
  }
}


export default Facets;
