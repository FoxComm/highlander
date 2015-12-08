import _ from 'lodash';
import React, { PropTypes } from 'react';
import Currency from '../../common/currency';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { autobind } from 'core-decorators';

export default class Row extends React.Component {
  constructor(...args) {
    super(...args);

    this.state = {
      showDetails: false
    };
  }

  static propTypes = {
    isEditing: PropTypes.bool.isRequired,
    icon: PropTypes.node.isRequired,
    summary: PropTypes.node.isRequired,
    amount: PropTypes.number,
    details: PropTypes.func.isRequired,
    editAction: PropTypes.node.isRequired,
  };

  get editAction() {
    if (this.props.isEditing) {
      return (
        <TableCell>
          {this.props.editAction}
        </TableCell>
      );
    }
  }

  get amount() {
    const amount = this.props.amount;
    return _.isNumber(amount) ? <Currency value={amount} /> : null;
  }

  @autobind
  toggleDetails() {
    this.setState({
      ...this.state,
      showDetails: !this.state.showDetails
    });
  }

  render() {
    const { icon, summary, amount, editAction } = this.props;
    let details = null;
    let nextDetailAction = null;

    if (this.state.showDetails) {
      nextDetailAction = 'up';
      details = this.props.details();
    } else {
      nextDetailAction = 'down';
      details = '';
    }

    return (
      <TableRow>
        <TableCell>
          <div className="fc-payment-method fc-grid">
            <div className="fc-left">
              <i className={`icon-chevron-${nextDetailAction}`} onClick={this.toggleDetails}></i>
            </div>
            <div className="fc-col-md-8-12">
              <div className="fc-left">
                <img className="fc-icon-lg" src={icon}></img>
              </div>
              {summary}
            </div>
            <div className="fc-payment-method-details">
              <div className="fc-push-md-3-12 fc-col-md-9-12">
                {details}
              </div>
            </div>
          </div>
        </TableCell>
        <TableCell>
          <div>
            <div>
              {this.amount}
            </div>
          </div>
        </TableCell>
        {this.editAction}
      </TableRow>
    );
  }
};
