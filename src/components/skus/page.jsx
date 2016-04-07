/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import { Link, IndexLink } from '../link';
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import LocalNav from '../local-nav/local-nav';

// actions
import * as SkuActions from '../../modules/skus/details';

// types
import type { FullSku, SkuState } from '../../modules/skus/details';

type Props = {
  actions: {
    fetchSku: (code: string, context?: string) => void,
    updateSku: (sku: FullSku, context?: string) => void,
  },
  children: Element,
  params: { skuCode: string },
  skus: SkuState,
};

type State = {
  sku: ?FullSku,
};

export class SkuPage extends Component<void, Props, State> {
  static propTypes = {
    actions: PropTypes.shape({
      fetchSku: PropTypes.func.isRequired,
      updateSku: PropTypes.func.isRequired,
    }).isRequired,
    children: PropTypes.node,
    params: PropTypes.shape({
      skuCode: PropTypes.string.isRequired,
    }),
    skus: PropTypes.object,
  };

  state: State;

  constructor(props: Props, ...args: any) {
    super(props, ...args);
    this.state = { sku: this.props.skus.sku };
  }

  componentDidMount() {
    this.props.actions.fetchSku(this.props.params.skuCode);
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState({ sku: nextProps.skus.sku });
  }

  @autobind
  handleChange(sku: FullSku) {
    this.setState({ sku });
  }

  @autobind
  handleSubmit() {
    if (this.state.sku) {
      this.props.actions.updateSku(this.state.sku);
    }
  }

  render(): Element {
    const { params } = this.props;
    const { isUpdating } = this.props.skus;
    const title = params.skuCode.toUpperCase();
    const children = React.cloneElement(this.props.children, {
      ...this.props.children.props,
      code: params.skuCode,
      entity: { entityId: params.skuCode, entityType: 'sku' },
      onChange: this.handleChange,
      sku: this.state.sku,
    });

    return (
      <div>
        <PageTitle title={title}>
          <PrimaryButton
            className="fc-product-details__save-button"
            type="submit"
            disabled={isUpdating}
            isLoading={isUpdating}
            onClick={this.handleSubmit}>
            Save
          </PrimaryButton>
        </PageTitle>
        <LocalNav>
          <IndexLink to="sku-details" params={params}>Details</IndexLink>
          <Link to="sku-images" params={params}>Images</Link>
          <Link to="sku-notes" params={params}>Notes</Link>
          <Link to="sku-activity-trail" params={params}>Activity Trail</Link>
        </LocalNav>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            {children}
          </div>
        </div>
      </div>
    );
  }
}

export default connect(
  state => ({ skus: state.skus.details }),
  dispatch => ({ actions: bindActionCreators(SkuActions, dispatch) })
)(SkuPage);
