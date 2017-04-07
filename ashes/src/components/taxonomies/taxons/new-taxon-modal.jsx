/* @flow */

//libs
import compact from 'lodash/compact';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import ObjectFormInner from 'components/object-form/object-form-inner';

// helpers
import { createEmptyTaxon } from 'paragons/taxon';

// styles
import s from './taxons.css';

type Props = {
  taxonomy: Taxonomy,
  taxon: TaxonDraft,
  onConfirm: (taxon: TaxonDraft) => any,
};

type State = {
  taxon: TaxonDraft,
}

export default class NewTaxonModal extends Component {
  props: Props;

  state: State = {
    taxon: createEmptyTaxon(),
  };

  @autobind
  handleTaxonUpdate(newAttributes: Attributes) {
    this.setState({
      taxon: assoc(this.state.taxon, 'attributes', newAttributes),
    });
  }

  @autobind
  handleConfirm() {
    this.props.onConfirm(this.state.taxon);
  }

  render() {
    const { taxonomy, ...rest } = this.props;

    const fields = compact([
      'name',
      taxonomy.hierarchical ? 'parent' : null,
    ]);

    const body = (
      <ObjectFormInner
        onChange={this.handleTaxonUpdate}
        fieldsToRender={fields}
        attributes={this.state.taxon.attributes}
      />
    );

    return (
      <ConfirmationDialog
        className={s.modal}
        header="New value"
        body={body}
        confirm="Save and Add Value"
        confirmAction={this.handleConfirm}
        {...rest}
      />
    );
  }
}
