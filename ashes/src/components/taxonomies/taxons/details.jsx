// @flow

// lib
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';
import TaxonListWidget from './taxon-list-widget';

import type { Renderers } from 'components/object-page/object-details-deux';

type ReduceResult = { [key: string]: Array<number | string> };

const buildTaxonsDropDownItems = (taxons: TaxonsTree, prefix: string, sep: string, finale: ReduceResult = {}) =>
  taxons.reduce((res: ReduceResult, node: TaxonNode) => {
    const name = `${prefix}${getName(node.taxon)}`;
    res = assoc(res, [node.taxon.id], [node.taxon.id, name]);

    if (!isEmpty(node.children)) {
      res = merge(res, buildTaxonsDropDownItems(node.children, `${name}${sep}`, sep, res));
    }

    return res;
  }, finale);

export default class TaxonDetails extends Component {
  /*
   * should be
   * props: ObjectPageChildProps<Taxon> & {
   *  taxonomy: Taxonomy,
   * }
   * but flow does not understand that props are of type ObjectPageChildProps<Taxon> and throw an error in
   * <ObjectDetailsDeux
   *   {...this.props}
   *   renderers={this.renderers}
   * />
   */
  props: ObjectPageChildProps<Taxon>;

  @autobind
  handleParentChange(value: number) {
    const newTaxon = assoc(this.props.object, ['location', 'parent'], parseInt(value, 10));

    this.props.onUpdateObject(newTaxon);
  }

  get parentItems(): Array<Array<number | string>> {
    const taxons = get(this.props, 'taxonomy.taxons', []);

    return values(buildTaxonsDropDownItems(taxons, '', ' :: '));
  }

  @autobind
  renderLocation() {
    return (
      <FormField
        className="fc-object-form__field"
        labelClassName="fc-object-form__field-label"
        label="Parent"
      >
        <Dropdown
          id="parentId"
          name="parentId"
          value={get(this.props.object, 'location.parent', '')}
          onChange={this.handleParentChange}
          items={this.parentItems}
        />
      </FormField>
    );
  }

  @autobind
  renderTaxonListWidget() {
    const params = get(this.props, 'params', {});
    return (
      <TaxonListWidget
        context={params.context}
        taxonomyId={params.taxonomyId}
        activeTaxonId={params.taxonId}
      />
    );
  };

  get renderers(): Renderers {
    return {
      location: this.renderLocation,
      taxonList: this.renderTaxonListWidget,
    };
  }

  render() {
    return (
      <ObjectDetailsDeux
        {...this.props}
      />
    );
  }
}
