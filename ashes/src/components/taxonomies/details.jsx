// @flow

// lib
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';

// types
import type { Taxonomy } from 'paragons/taxonomy';

const layout = require('./layout.json');

type Props = {
  details: {
    taxonomy: ?Taxonomy,
  },
};

class TaxonomyDetails extends Component {
  props: Props;

// type Props = {
//   layout: Layout,
//   title: string,
//   plural: string,
//   object: ObjectView,
//   schema: Object,
//   onUpdateObject: (object: ObjectView) => void,
// };

  render(): Element {
    const { taxonomy } = this.props.details;
    if (!taxonomy) {
      return <div></div>;
    }

    const schema = {
      "type": "object",
      "title": "Taxonomy",
      "$schema": "http://json-schema.org/draft-04/schema#",
      "properties": {
        "attributes": {
          "type": "object",
          "required": [
            "name"
          ],
          "properties": {
            "name": {
              "type": "string",
              "minLength": 1
            },
            "activeTo": {
              "type": [
                "string",
                "null"
              ],
              "format": "date-time"
            },
            "activeFrom": {
              "type": [
                "string",
                "null"
              ],
              "format": "date-time"
            },
            "description": {
              "type": "string",
              "widget": "richText"
            },
          },
          "description": "Taxonomy attributes itself"
        }
      }
    };

    return (
      <ObjectDetailsDeux
        layout={layout}
        title="taxonomy"
        plural="taxonomies"
        object={taxonomy}
        schema={schema}
        onUpdateObject={(o) => {}}
      />
    );
  }
}

const mapStateToProps = state => {
  return {
    details: state.taxonomies.details,
  };
};

export default connect(mapStateToProps, null)(TaxonomyDetails);
