// @flow

// lib
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import FormField from 'components/forms/formfield';
import ObjectDetailsDeux from 'components/object-page/object-details-deux';
import TaxonsDropdown from '../taxons-dropdown';

export default class TaxonDetails extends Component {
  props: ObjectPageChildProps<Taxon> & {
    taxonomy: Taxonomy,
  };

  @autobind
  handleParentChange(id: ?number) {
    const newTaxon = assoc(this.props.object, ['location', 'parent'], id);

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
        <TaxonsDropdown
          context={this.props.params.context}
          taxonomy={this.props.taxonomy}
          taxon={this.props.object}
          onChange={this.handleParentChange}
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
    // workaround for flow strange behavior with intersection types
    const props: ObjectPageChildProps<Taxon> = this.props;

    return (
      <ObjectDetailsDeux
        {...props}
        renderers={this.renderers}
      />
    );
  }
}
