
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

class TotalCounter extends React.Component {
  static propTypes = {
    url: PropTypes.string.isRequired,
    isFetching: PropTypes.oneOf([null, true, false]).isRequired,
    entitiesCount: PropTypes.number.isRequired,
    fetch: PropTypes.func.isRequired,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      showCounterForFetching: false,
    };
  }

  componentDidMount() {
    const { isFetching, fetch, url} = this.props;
    if (isFetching === null) {
      fetch(url);
    }
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.isFetching === true) {
      this.setState({
        showCounterForFetching: this.props.isFetching === false,
      });
    }
  }

  render() {
    const { entitiesCount, isFetching } = this.props;
    let count = '';
    if (isFetching === false || (isFetching === true && this.state.showCounterForFetching)) {
      count = entitiesCount;
    }

    return <span>{count}</span>;
  }
}

function makePropsMapper(getLSState) {
  return state => {
    const {selectedSearch, savedSearches} = getLSState(state);

    return {
      entitiesCount: _.get(savedSearches, [selectedSearch, 'results', 'total']),
      isFetching: _.get(savedSearches, [selectedSearch, 'isFetching']),
    };
  };
}

export default function makeTotalCounter(getLSState, actions) {
  return connect(makePropsMapper(getLSState), actions)(TotalCounter);
}
