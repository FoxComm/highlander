// @flow

// lib
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';
import { SliderCheckbox } from '../checkbox/checkbox'

//styles
import styles from './taxonomy.css';

const layout = require('./layout.json');

type Props = {
  schema: ObjectSchema,
  taxonomy: ?Taxonomy,
  onUpdateObject: (object: ObjectView) => void,
};


export default class TaxonomyDetails extends React.Component {

  @autobind
  renderHierarchical() {
    if (this.props.taxonomy.id) {
      return null
    }
    return (
      <div styleName="toggle-container">
        <label>Hierarchical</label>
        <div>
          <SliderCheckbox
            id="hierarchicalType"
            onChange={this.handleHierarchicalChange}
            checked={this.props.taxonomy.hierarchical}
          />
        </div>
      </div>
    )
  }

  @autobind
  handleHierarchicalChange() {
    const newTaxonomy = assoc(
      this.props.taxonomy,
      'hierarchical',
      !this.props.taxonomy.hierarchical
    );
    this.props.onUpdateObject(newTaxonomy);
  }

  render () {
  const { schema, taxonomy, onUpdateObject } = this.props;
    if (!taxonomy) {
      return <div></div>;
    }
    return (
      <ObjectDetailsDeux
        layout={layout}
        title="taxonomy"
        plural="taxonomies"
        object={taxonomy}
        schema={schema}
        onUpdateObject={onUpdateObject}
        renderers={{
          hierarchical: this.renderHierarchical
        }}
      />
    );
  }
};

