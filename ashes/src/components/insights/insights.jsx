// libs
import { get } from 'sprout-data';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { LineChart} from 'react-d3';

// components
import ErrorAlerts from '../alerts/error-alerts';
import WaitAnimation from '../common/wait-animation';
import { SectionTitle } from '../section-title';

// redux
import * as InsightActions from '../../modules/insights';

@connect(state => state.insights, InsightActions)
export default class Insights extends React.Component {

  static propTypes = {
    key: PropTypes.string,
    values: PropTypes.array.isRequired,
    from: PropTypes.number,
    to: PropTypes.number,
    size_sec: PropTypes.number,
    step_sec: PropTypes.number,
    err: PropTypes.any,
    isFetching: PropTypes.bool,
    route: PropTypes.shape({
      action: PropTypes.string,
      idKey: PropTypes.string,
    }),
    fetchInsights: PropTypes.func.isRequired,
    resetInsights: PropTypes.func.isRequired,
  };

  componentDidMount() {
    const { key } = this.props;
    to = Math.floor(Date.now() / 1000)
    from = to - 300;
    size_sec = 1;
    step_sec = 1;
    this.props.resetInsights();
    this.props.fetchInsights(key, from, to, size_sec, step_sec);
  }

  get content() {
    const { key, values, from, to, stize_sec, step_sec, err, isFetching = null } = this.props;

    const data = {
      name: key,
      values: values,
    };

    if (isFetching === false) {
      if (!err) {
        return <LineChart
          legend={true}
          data={data}
          width='100%'
          height={400}
          title={key}
          xAxisLabel="Time"
          yAxisLabel="Count"
          />;
      } else {
        return <ErrorAlerts error={err} />;
      }
    } else if (isFetching === true) {
      return <WaitAnimation />;
    }
  }

  render() {
    return (
      <div className="fc-insights-page">
        <SectionTitle title="Insights" />
        {this.content}
      </div>
    );
  }
}
