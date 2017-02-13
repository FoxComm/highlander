/* @flow */
import _ from 'lodash';
import React, { Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import Coupons from '../coupons/coupons'
import ErrorAlerts from '../alerts/error-alerts';
import WaitAnimation from '../common/wait-animation';
import { SectionTitle } from '../section-title';
import { PrimaryButton } from '../../components/common/buttons';

export default class PromoCouponsPage extends React.Component {

	@autobind
	logProps() {
		console.log(this.props)
	}

  render() {
    return (
      <div className="fc-promotion-coupons-page">
        <SectionTitle title="Coupons">
        	<PrimaryButton onClick={this.logProps} icon="add"/>
        </SectionTitle>
        <Coupons/>
      </div>
    );
  }  
};
