/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { assoc, dissoc } from 'sprout-data';
import { autoAssignOptions } from 'paragons/variants';
import { productVariantId } from 'paragons/product';

// components
import ContentBox from 'components/content-box/content-box';
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import OptionEntry from './option-entry';
import OptionEditDialog from './option-edit-dialog';

// styles
import styles from './option-list.css';

// types
import type { Option, OptionValue, Product } from 'paragons/product';
import type { ProductVariant } from 'modules/product-variants/details';

type Props = {
  options: Array<Option>,
  updateOptions: Function,
  product: Product,
};

type EditOption = {
  id: string|number,
  option: Option,
};

type DeletingContext = {
  id: number,
  affectedSkus: Array<ProductVariant>,
  deletingValueContext?: {
    option: Option,
    value: OptionValue
  },
}

type State = {
  editOption: ?EditOption,
  deletingContext?: DeletingContext,
};

class OptionList extends Component {
  props: Props;

  state: State = {
    editOption: null,
    isDeleteDialogVisible: false,
  };

  get actions(): Element {
    return (
      <a id="add-new-option-btn" styleName="action-icon" onClick={() => this.startEditOption('new')}>
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
    const option = (id !== 'new') ? this.props.options[id] : {
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

  getOptionsWithoutValue(id: number): Array<Option> {
    const { options } = this.props;

    const newOptions = options.slice();
    newOptions.splice(id, 1);

    return newOptions;
  }

  deleteOption(id: number): void {
    this.props.updateOptions(this.getOptionsWithoutValue(id));
  }

  @autobind
  handleUpdateOption(id: string|number, option: Option, deletingValue: ?OptionValue): void {
    let needConfirmation = false;
    let affectedSkus = [];

    if (deletingValue) {
      const newOptions = assoc(this.props.options, id, option);
      affectedSkus = this.getRemovedSkusByNewOptions(newOptions);
      needConfirmation = affectedSkus.length > 0;
    }

    if (needConfirmation && deletingValue) {
      this.setState({
        deletingContext: {
          affectedSkus,
          // $FlowFixMe: id is number here
          id,
          deletingValueContext: {
            option,
            value: deletingValue,
          }
        },
      });
    } else {
      this.updateOption(id, option);
    }
  }

  @autobind
  handleDeleteClick(id: number) {
    const newVariants = this.getOptionsWithoutValue(id);
    const affectedSkus = this.getRemovedSkusByNewOptions(newVariants);
    if (affectedSkus.length > 0) {
      this.setState({
        deletingContext: {
          affectedSkus,
          id,
        }
      });
    } else {
      this.deleteOption(id);
    }
  }


  @autobind
  updateOption(id: string|number, option: Option): void {
    const { options } = this.props;

    const newOptions = id == 'new' ? [...options, option] : assoc(options, id, option);

    this.setState({
      editOption: null,
    }, () => this.props.updateOptions(newOptions));
  }

  @autobind
  cancelEditOption(): void {
    this.setState({
      editOption: null,
    });
  }

  closeDeleteDialog() {
    this.setState({
      deletingContext: void 0,
    });
  }

  renderOptions(options: Array<Option>): Array<Element> {
    return _.map(options, (option, index) => {
      const key = _.get(option.attributes, 'name.v', index);
      return (
        <OptionEntry
          key={key}
          id={index}
          option={option}
          editOption={this.startEditOption}
          deleteOption={this.handleDeleteClick}
          confirmAction={this.handleUpdateOption}
        />
      );
    });
  }

  getRemovedSkusByNewOptions(newOptions: Array<Option>): Array<ProductVariant> {
    const newProduct = autoAssignOptions(this.props.product, newOptions);
    const newVariants = _.keyBy(newProduct.variants, productVariantId);

    return _.filter(this.props.product.variants, variant => {
      const hasRealCode = !!_.get(variant.attributes, 'code.v');
      return hasRealCode && !(productVariantId(variant) in newVariants);
    });
  }

  confirmDeletion() {
    if (!this.state.deletingContext) return;

    const { id, deletingValueContext } = this.state.deletingContext;
    if (!deletingValueContext) {
      this.deleteOption(id);
    } else {
      this.updateOption(id, deletingValueContext.option);
    }
    this.closeDeleteDialog();
  }

  get deletionDialog(): ?Element {
    if (!this.state.deletingContext) return;

    const { id, affectedSkus, deletingValueContext } = this.state.deletingContext;
    const skuListForDeletion = affectedSkus.map(sku => {
      return (
        <li key={sku.attributes.code.v}><tt>{sku.attributes.code.v}</tt></li>
      );
    });
    let removeTarget;
    let removeTargetTitle;
    if (!deletingValueContext) {
      const deletingOption = this.props.options[id];
      const optionName = _.get(deletingOption, 'attributes.name.v');
      removeTargetTitle = 'option';
      removeTarget = `option ${optionName}`;
    } else {
      const { value } = deletingValueContext;
      removeTargetTitle = 'option value';
      removeTarget = `option value ${value.name}`;
    }

    const confirmation = (
      <div>
        Are you sure you want to remove {removeTarget} from the product?<br/>
        This action will remove following SKUs from product:
        <ul styleName="deleting-skus">
          {skuListForDeletion}
        </ul>
        This action will <i>not</i> archive these SKUs.
      </div>
    );
    return (
      <ConfirmationDialog
        isVisible={true}
        header={`Remove ${removeTargetTitle} from product?`}
        body={confirmation}
        cancel="Cancel"
        confirm="Yes, Remove"
        onCancel={() => this.closeDeleteDialog()}
        confirmAction={() => this.confirmDeletion()}
      />
    );
  }

  render(): Element {
    const options = this.renderOptions(this.props.options);
    const content = _.isEmpty(options) ? this.emptyContent : options;

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
        {this.deletionDialog}
      </ContentBox>
    );
  }
}

export default OptionList;
