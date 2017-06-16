/* @flow */

// libs
import { omit } from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import ObjectFormInner from 'components/object-form/object-form-inner';
import FormField from 'components/forms/formfield';
import TaxonsDropdown from '../taxons-dropdown';

// helpers
import { createEmptyTaxon } from 'paragons/taxon';

// styles
import s from './taxons.css';

type Props = {
  context: string,
  taxonomy: Taxonomy,
  onConfirm: (taxon: TaxonDraft) => any,
};

type State = {
  taxon: TaxonDraft,
}

const omitProps = [
  'context',
  'taxonomy',
  'onConfirm',
];

export default class NewTaxonModal extends Component {
  props: Props;

  state: State = {
    taxon: createEmptyTaxon(),
  };

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.isVisible && !this.props.isVisible) {
      this.state.taxon = createEmptyTaxon();
    }
  }

  @autobind
  handleTaxonUpdate(newAttributes: Attributes) {
    this.setState({
      taxon: assoc(this.state.taxon, 'attributes', newAttributes),
    });
  }

  @autobind
  handleParentUpdate(id: ?number) {
    this.setState({
      taxon: assoc(this.state.taxon, ['location', 'parent'], id),
    });
  }

  @autobind
  handleConfirm() {
    this.props.onConfirm(this.state.taxon);
  }

  get parentInput() {
    if (!this.props.taxonomy.hierarchical) {
      return null;
    }

    const taxon = {
      ...this.state.taxon,
      id: null,
    };

    return (
      <FormField
        className="fc-object-form__field"
        labelClassName="fc-object-form__field-label"
        label="Parent"
      >
        <TaxonsDropdown
          context={this.props.context}
          taxonomy={this.props.taxonomy}
          taxon={taxon}
          onChange={this.handleParentUpdate}
        />
      </FormField>
    );
  }

  render() {
    const props = omit(this.props, omitProps);

    return (
      <ConfirmationModal
        className={s.modal}
        title="New value"
        confirmLabel="Save and Add Value"
        onConfirm={this.handleConfirm}
        {...props}
      >
        <ObjectFormInner
          onChange={this.handleTaxonUpdate}
          fieldsToRender={['name']}
          attributes={this.state.taxon.attributes}
        />
        {this.parentInput}
      </ConfirmationModal>
    );
  }
}
