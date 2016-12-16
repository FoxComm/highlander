// libs
import { get } from 'sprout-data';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { LineChart} from 'rd3';
import * as d3 from 'd3';

// components
import ErrorAlerts from '../alerts/error-alerts';
import WaitAnimation from '../common/wait-animation';
import { SectionTitle } from '../section-title';
import _ from 'lodash';

// redux
import * as InsightActions from '../../modules/insights';

const verbs = {
  product: {
  list: 'Shown in Category',
  pdp: 'Viewed Pdp'
  }
}; 

const colors = ["#2ca02c", "#ff7f0e"];

@connect((state, props) => ({insights: state.insights}), InsightActions)
export default class Insights extends React.Component {

  static propTypes = {
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
      ]),
      entityType: PropTypes.string,
    }),
    insights: PropTypes.shape({
        insightKey: PropTypes.string,
        values: PropTypes.object,
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
    fetchInsights: PropTypes.func.isRequired
  };

  componentDidMount() {
    const channel = 1;
    const keys = _.map(verbs[this.props.entity.entityType], (title, verb) => { 
      const key = `track.${channel}.${this.props.entity.entityType}.${this.props.entity.entityId}.${verb}`;
      return { 
          key: key,
          title: title,
          verb: verb,
      };
    });

    const to = Math.floor(Date.now() / 1000) + 300;
    const from = to - (300*20);
    const sizeSec = 60;
    const stepSec = 60;
    this.props.fetchInsights(keys, from, to, sizeSec, stepSec);
  }

  get content() {
    const { insights } = this.props;

    if (insights.isFetching === false) {
      if (!insights.err) {

        const data = _.map(insights.values, (rawValues, k) => {
          const values = rawValues.map((v) => {
            return {
              x: new Date(v.x * 1000),
              y: v.y
            };
          });

          const verb = _.find(insights.keys, {'key': k});

          return {
            name: verb.title,
            values: values,
            strokeWidth: verb.strokeWidth,
            strokeDashArray: verb.strokeDashArray,
          };
        });

        return <LineChart
          legend={true}
          data={data}
          colors={(idx) => colors[idx]}
          width='100%'
          height={400}
          legendOffset={200}
          viewBoxObject={{
              x: 0,
              y: 0,
              width: 800,
              height: 400
          }}
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
