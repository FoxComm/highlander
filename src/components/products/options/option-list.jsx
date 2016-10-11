/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { assoc, dissoc } from 'sprout-data';
import { autoAssignVariants } from 'paragons/variants';
import { skuId } from 'paragons/product';

// components
import ContentBox from 'components/content-box/content-box';
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import OptionEntry from './option-entry';
import OptionEditDialog from './option-edit-dialog';

// styles
import styles from './option-list.css';

// types
import type { Option, Product } from 'paragons/product';

type Props = {
  variants: Array<Option>,
  updateVariants: Function,
  product: Product,
};

type EditOption = {
  id: string|number,
  option: Option,
};

type State = {
  editOption: ?EditOption,
  optionToDelete?: number,
};

class OptionList extends Component {
  props: Props;

  state: State = {
    editOption: null,
    isDeleteDialogVisible: false,
  };

  get actions(): Element {
    return (
      <a styleName="action-icon" onClick={() => this.startEditOption('new')}>
        <i className="icon-add" />
      </a>
    );
  }

  get emptyContent(): Element {
    return (
      <div className="fc-content-box__empty-text">
        This product does not have options.
      </div>
    );
  }

  @autobind
  startEditOption(id: any): void {
    const option = (id !== 'new') ? this.props.variants[id] : {
      attributes: {
        name: {
          t: 'string',
          v: ''
        },
        type: {
          t: 'string',
          v: '',
        }
      },
      values: [],
    };

    const editOption = { id, option };

    this.setState({
      editOption
    });
  }

  getVariantsWithoutOption(id: number): Array<Option> {
    const { variants } = this.props;

    const newVariants = variants.slice();
    newVariants.splice(id, 1);

    return newVariants;
  }

  deleteOption(id: number): void {
    this.props.updateVariants(this.getVariantsWithoutOption(id));
  }

  @autobind
  updateOption(id: string|number, option: Option): void {
    const { variants } = this.props;

    const newVariants = id == 'new' ? [...variants, option] : assoc(variants, id, option);

    this.setState({
      editOption: null,
    }, () => this.props.updateVariants(newVariants));
  }

  @autobind
  cancelEditOption(): void {
    this.setState({
      editOption: null,
    });
  }

  @autobind
  handleDeleteClick(id: number) {
    this.setState({
      optionToDelete: id,
    });
  }

  closeDeleteDialog() {
    this.setState({
      optionToDelete: void 0
    });
  }

  renderOptions(variants: Array<Option>): Array<Element> {
    return _.map(variants, (option, key) => {
      const reactKey = `product-variant-${key}`;
      return (
        <OptionEntry
          key={reactKey}
          id={key}
          option={option}
          editOption={this.startEditOption}
          deleteOption={this.handleDeleteClick}
          confirmAction={this.updateOption}
        />
      );
    });
  }

  getAffectedSkusByDeletion(optionId: number) {
    const newVariants = this.getVariantsWithoutOption(optionId);
    const newProduct = autoAssignVariants(this.props.product, newVariants);
    const newSkus = _.keyBy(newProduct.skus, skuId);

    return _.filter(this.props.product.skus, sku => {
      const hasRealCode = !!_.get(sku.attributes, 'code.v');
      return hasRealCode && !(skuId(sku) in newSkus);
    });
  }

  get deleteDialog(): ?Element {
    if (this.state.optionToDelete == void 0 || !this.props.variants[this.state.optionToDelete]) {
      return null;
    }
    const affectedSkus = this.getAffectedSkusByDeletion(this.state.optionToDelete);
    const skuListForDeletion = affectedSkus.map(sku => {
      return (
        <li key={sku.attributes.code.v}><tt>{sku.attributes.code.v}</tt></li>
      );
    });
    // $FlowFixMe: optionToDelete is number here
    const deletingOption = this.props.variants[this.state.optionToDelete];
    const optionName = _.get(deletingOption.attributes, 'name.v');
    const confirmation = (
      <div>
        Are you sure you want to remove option {optionName} from the product?<br/>
        This action will remove following SKUs from product:
        <ul styleName="deleting-skus">
          {skuListForDeletion}
        </ul>
        This action will <i>not</i> archive these SKUs.
      </div>
    );
    return (
      <ConfirmationDialog
        isVisible={this.state.optionToDelete != void 0}
        header="Remove option from product?"
        body={confirmation}
        cancel="Cancel"
        confirm="Yes, Remove"
        cancelAction={() => this.closeDeleteDialog()}
        // $FlowFixMe: optionToDelete is number here
        confirmAction={() => this.deleteOption(this.state.optionToDelete)}
      />
    );
  }


  render(): Element {
    const variants = this.renderOptions(this.props.variants);
    const content = _.isEmpty(variants) ? this.emptyContent : variants;

    const optionDialog = this.state.editOption && (
      <OptionEditDialog
        option={this.state.editOption}
        cancelAction={this.cancelEditOption}
        confirmAction={this.updateOption}
      />
    );

    return (
      <ContentBox title="Options" actionBlock={this.actions}>
        {content}
        {this.state.editOption && optionDialog}
        {this.deleteDialog}
      </ContentBox>
    );
  }
}

export default OptionList;
