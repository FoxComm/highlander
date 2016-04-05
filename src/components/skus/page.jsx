/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';

import { Link, IndexLink } from '../link';
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import LocalNav from '../local-nav/local-nav';

type Params = { skuCode: string };
type Props = { children: Element, params: Params };

export default class SkuPage extends Component<void, Props, void> {
  static propTypes = {
    children: PropTypes.node,
    params: PropTypes.shape({
      skuCode: PropTypes.string.isRequired,
    }),
  };

  render(): Element {
    const { params } = this.props;
    const title = params.skuCode.toUpperCase();
    const children = React.cloneElement(this.props.children, {
      ...this.props.children.props,
      code: params.skuCode,
      entity: { entityId: params.skuCode, entityType: 'sku' },
    });

    return (
      <div>
        <PageTitle title={title}>
          <PrimaryButton>Save</PrimaryButton>
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
