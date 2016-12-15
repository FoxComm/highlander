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

@connect(state => ({insights: state.insights}), InsightActions)
export default class Insights extends React.Component {

  static propTypes = {
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
      ]),
      entityType: PropTypes.string,
    }),
    productId: PropTypes.number,
    insights: PropTypes.shape({
        insightKey: PropTypes.string,
        values: PropTypes.array,
        from: PropTypes.number,
        to: PropTypes.number,
        sizeSec: PropTypes.number,
        stepSec: PropTypes.number,
        err: PropTypes.any,
        isFetching: PropTypes.bool,
        route: PropTypes.shape({
            action: PropTypes.string,
            idKey: PropTypes.string,
    })}),
    fetchInsights: PropTypes.func.isRequired,
    resetInsights: PropTypes.func.isRequired
  };

  componentDidMount() {
    const channel = 1;
    const verb = 'list';
    const insightKey = `track.${channel}.${this.props.entity.entityType}.${this.props.entity.entityId}.${verb}`;
    const to = Math.floor(Date.now() / 1000);
    const from = to - (300*20);
    const sizeSec = 60;
    const stepSec = 60;
    this.props.fetchInsights(insightKey, from, to, sizeSec, stepSec);
  }

  get content() {
    const { insights } = this.props;

    if (insights.isFetching === false) {
      if (!insights.err) {

        const values = insights.values.map((v) => {
          return {
            x: new Date(v.x * 1000),
            y: v.y
          };
        });
        const data = [{
          name: "Views",
          values: values
        }];
        console.log("DATA");
        console.log(data);
        return <LineChart
          legend={true}
          data={data}
          width={800}
          height={400}
          title="Last 60 Minutes"
          xAxisLabel="Time"
          yAxisLabel="Count"
          />;
      } else {
        return <ErrorAlerts error={insights.err} />;
      }
    } else {
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
