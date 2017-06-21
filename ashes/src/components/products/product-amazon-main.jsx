/**
 * @flow
 * This component responsible for category suggester handling
 * All other data gets from props, and all other actions goes up to props-handlers
 */

// libs
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// components
import Spinner from 'components/core/spinner';
import ObjectFormInner from 'components/object-form/object-form-inner';
import Typeahead from 'components/typeahead/typeahead';
import { CategoryItem } from './category-item';
import Icon from 'components/core/icon';

// types
import type { SuggestItem } from './selector';
import type { AttrSchema } from 'paragons/object';

// actions
import { fetchSuggest } from 'modules/channels/amazon';

// selectors
import { getSuggest } from './selector';

// styles
import s from './product-amazon.css';

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(
      {
        fetchSuggest,
      },
      dispatch
    ),
  };
}

function mapStateToProps(state) {
  const { suggest } = state.channels.amazon;

  return {
    fetchingSuggest: _.get(state.asyncActions, 'fetchSuggest.inProgress'),
    suggest: getSuggest(suggest),
  };
}

const AMAZON_APPROVE_LINK = [
  `https://sellercentral.amazon.com/gp/case-dashboard/workflow-details.html/`,
  `?extraArguments={%22caseCategory%22%3A%22apparel%22,%22workflowId%22:%2276%22}`,
].join('');

type Actions = {
  fetchSuggest: Function,
};

type Props = {
  product: Product,
  schema: AttrSchema,
  actions: Actions,
  fetchingSuggest: boolean,
  suggest: Array<SuggestItem>,
  onChange: Function,
};

type State = {
  categoryId: string,
  categoryPath: string,
};

class ProductAmazonMain extends Component {
  props: Props;

  state: State = {
    categoryId: _.get(this.props.product, 'attributes.nodeId.v', ''),
    categoryPath: _.get(this.props.product, 'attributes.nodePath.v', ''),
  };

  componentWillUpdate(nextProps) {
    const nodeId = _.get(this.props.product, 'attributes.nodeId.v', null);
    const nextNodeId = _.get(nextProps.product, 'attributes.nodeId.v', null);
    const nextNodePath = _.get(nextProps.product, 'attributes.nodePath.v', null);

    if (nextNodeId && nextNodeId != nodeId && nextNodePath) {
      this.setState({ categoryPath: nextNodePath });
    }
  }

  renderFields() {
    const { categoryId, categoryPath } = this.state;
    const { schema, product: { attributes } } = this.props;

    if (!schema || !schema.properties) {
      if (categoryId) {
        return <Spinner />;
      }

      return null;
    }

    const p = schema.properties.attributes.properties;
    // @todo should we use the schema as layout?
    const pKeys = _.keys(p); // All Amazon-specific field names, e.g. ['title', 'description' ...]
    const amazonVoidAttrs = _.mapValues(p, attr => ({
      t: 'string',
      v: '',
    }));
    amazonVoidAttrs.nodeId.v = categoryId;
    amazonVoidAttrs.nodePath.v = categoryPath;
    const amazonExistedAttrs = _.pick(attributes, pKeys);
    const amazonAllAttrs = { ...amazonVoidAttrs, ...amazonExistedAttrs };

    return (
      <ObjectFormInner
        onChange={this.handleChange}
        attributes={amazonAllAttrs}
        schema={schema.properties && schema.properties.attributes}
        className={s.mainForm}
      />
    );
  }

  @autobind
  handleChange(nextAttributes) {
    const { product, onChange } = this.props;

    onChange(
      assoc(product, 'attributes', {
        ...product.attributes,
        ...nextAttributes,
      })
    );
  }

  @autobind
  handleCatPick({ id, path }) {
    this.setCat(id, path);
  }

  setCat(id, path) {
    this.setState({ categoryId: id, categoryPath: path });

    const nextAttributes = {
      nodeId: { t: 'string', v: id },
      nodePath: { t: 'string', v: path },
    };

    this.handleChange(nextAttributes);
  }

  @autobind
  handleFetch(text) {
    const { product } = this.props;
    const title = _.get(product, 'attributes.title.v', '');

    return this.props.actions.fetchSuggest(title, text);
  }

  render() {
    const { categoryPath } = this.state;
    const { suggest, fetchingSuggest } = this.props;

    return (
      <div>
        <div className={s.suggesterWrapper}>
          <div className={s.fieldLabel}>Amazon Category</div>
          <Typeahead
            className={s.suggester}
            onItemSelected={this.handleCatPick}
            items={suggest}
            isFetching={fetchingSuggest}
            fetchItems={this.handleFetch}
            component={CategoryItem}
            initialValue={categoryPath}
            hideOnBlur
            placeholder="Start typing to search category..."
          />
          <div className={s.approve}>
            <span>
              <Icon name="warning" />
              {' '}
              You must be approved from Amazon to sell in the Clothing & Accesories category.
              {' '}
            </span>
            <a className={s.approveLink} href={AMAZON_APPROVE_LINK} target="_blank">Apply to sell in this category</a>.
          </div>
        </div>
        {this.renderFields()}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazonMain);
