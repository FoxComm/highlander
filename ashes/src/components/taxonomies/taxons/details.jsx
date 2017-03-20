// @flow

// lib

import get from 'lodash/get';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import FormField from 'components/forms/formfield';
import TextInput from 'components/forms/text-input';
import ObjectDetailsDeux from 'components/object-page/object-details-deux';

// types
import type { Renderers } from 'components/object-page/object-details-deux';

export default class TaxonDetails extends Component {
  props: ObjectPageChildProps<Taxon>;

  @autobind
  handleParentChange(value) {
    const newTaxon = assoc(this.props.object, ['location', 'parent'], parseInt(value, 10));

    this.props.onUpdateObject(newTaxon);
  }

  @autobind
  renderLocation() {
    return (
      <FormField
        className="fc-object-form__field"
        labelClassName="fc-object-form__field-label"
        label="Parent"
      >
        <TextInput
          id="parentId"
          name="parentId"
          onChange={this.handleParentChange}
          value={get(this.props.object, 'location.parent', '')}
        />
      </FormField>
    );
  }

  get renderers(): Renderers {
    return {
      location: this.renderLocation,
    };
  }

  render() {
    return (
      <ObjectDetailsDeux
        renderers={this.renderers}
        {...this.props}
      />
    );
  }
}
