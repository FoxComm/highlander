/**
 * This component responsible for category suggester handling
 * All other data gets from props, and all other actions goes up to props-handlers
 */

// libs
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import classNames from 'classnames';
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// components
import Form from 'components/forms/form';
import WaitAnimation from 'components/common/wait-animation';
import ObjectFormInner from 'components/object-form/object-form-inner';
import Typeahead from 'components/typeahead/typeahead';
import { CategoryItem } from './category-item';

// actions
import { fetchSuggest } from 'modules/channels/amazon';

// selectors
import { getSuggest, cat } from './selector';

// styles
import s from './product-amazon.css';

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({
      fetchSuggest,
    }, dispatch),
  };
}

function mapStateToProps(state) {
  const { suggest } = state.channels.amazon;

  return {
    fetchingSuggest: _.get(state.asyncActions, 'fetchSuggest.inProgress'),
    suggest: getSuggest(suggest),
  };
}

type Props = {
  product: Object,
  schema: Object,
  actions: Object,
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

    if (!schema) {
      if (categoryId) {
        return <WaitAnimation />;
      }

      return null;
    }

    const p = schema && schema.properties.attributes.properties;
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
        onChange={this._handleChange}
        attributes={amazonAllAttrs}
        schema={schema.properties.attributes}
        className={s.mainForm}
      />
    );
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
            onItemSelected={this._onCatPick}
            items={suggest}
            isFetching={fetchingSuggest}
            fetchItems={this._handleFetch}
            component={CategoryItem}
            initialValue={categoryPath}
          />
        </div>
        {this.renderFields()}
      </div>
    );
  }

  @autobind
  _handleChange(nextAttributes) {
    const { product, onChange } = this.props;

    onChange({
      ...product,
      attributes: {
        ...product.attributes,
        ...nextAttributes,
      },
    });
  }

  @autobind
  _onCatPick({ id, path }) {
    this._setCat(id, path);
  }

  _setCat(id, path) {
    this.setState({ categoryId: id, categoryPath: cat(path) });

    const nextAttributes = {
      nodeId: { t: 'string', v: id, },
      nodePath: { t: 'string', v: path, },
    };

    this._handleChange(nextAttributes);
  }

  @autobind
  _handleFetch(text) {
    const { product } = this.props;
    const title = _.get(product, 'attributes.title.v', '');

    this.props.actions.fetchSuggest(title, text);
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazonMain);
