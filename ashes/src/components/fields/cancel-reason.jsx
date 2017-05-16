// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import classNames from 'classnames';

// helpers
import { ReasonType } from '../../lib/reason-utils';

// data
import { fetchReasons } from '../../modules/reasons';

// components
import { Dropdown } from '../dropdown';


const mapStateToProps = ({reasons}, {reasonType}) => {
  return {
    reasons: _.get(reasons, ['reasons', reasonType], []),
  };
};

const mapDispatchToProps = (dispatch, {reasonType}) => {
  return {
    fetchReasons: () => dispatch(fetchReasons(reasonType)),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class CancelReason extends React.Component {

  static propTypes = {
    reasonType: PropTypes.oneOf(_.values(ReasonType)).isRequired,
    className: PropTypes.string,
    reasons: PropTypes.array,
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
    ]),
    onChange: PropTypes.func.isRequired,
    fetchReasons: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.props.fetchReasons();
  }

  render() {
    const {className, reasons, value, onChange} = this.props;

    return (
      <div className={classNames('fc-field-cancel-reason', className)}>
        <div>
          <label>
            Cancel Reason
            <span className="fc-field-cancel-reason__asterisk">*</span>
          </label>
        </div>
        <Dropdown className="fc-field-cancel-reason__selector"
                  name="cancellationReason"
                  placeholder="- Select -"
                  value={value}
                  onChange={onChange}
                  items={reasons.map(({id, body}) => [id, body])}
        />
      </div>
    );
  }
}
