// @flow

// lib
import get from 'lodash/get';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';
import { SliderCheckbox } from 'components/core/checkbox';

//styles
import styles from './taxonomy.css';

export default class TaxonomyDetails extends React.Component {
  props: ObjectPageChildProps<Taxonomy>;

  @autobind
  renderHierarchical() {
    if (get(this.props.object, 'id')) {
      return null;
    }

    return (
      <div styleName="toggle-container">
        <label>Hierarchical</label>
        <SliderCheckbox
          id="hierarchicalType"
          onChange={this.handleHierarchicalChange}
          checked={this.props.object.hierarchical}
        />
      </div>
    );
  }

  @autobind
  handleHierarchicalChange() {
    const newTaxonomy = assoc(this.props.object, 'hierarchical', !this.props.object.hierarchical);

    this.props.onUpdateObject(newTaxonomy);
  }

  get renderers(): Renderers {
    return {
      hierarchical: this.renderHierarchical,
    };
  }

  render() {
    return <ObjectDetailsDeux {...this.props} renderers={this.renderers} />;
  }
}
