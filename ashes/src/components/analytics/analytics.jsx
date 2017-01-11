// libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import {
  VictoryScatter,
  VictoryLine,
  VictoryChart,
  VictoryAxis,
  VictoryLabel,
  VictoryTooltip,
  VictoryGroup,
} from 'victory';
import moment from 'moment';
import _ from 'lodash';

// components
import ErrorAlerts from '../alerts/error-alerts';
import WaitAnimation from '../common/wait-animation';
import QuestionBox from './question-box';
import Currency from '../common/currency';
import TrendButton, { TrendType } from './trend-button';

// styles
import styles from './analytics.css';

// redux
import * as AnalyticsActions from '../../modules/analytics';

const verbs = {
  product: {
    list: 'Shown in Category',
    pdp: 'Viewed Pdp',
    cart: 'Added To Cart',
  }
}; 

const colors = ['#2ca02c', '#ff7f0e', '#662ca0'];

@connect((state, props) => ({analytics: state.analytics}), AnalyticsActions)
export default class Analytics extends React.Component {

  static propTypes = {
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
      ]),
      entityType: PropTypes.string,
    }),
    analytics: PropTypes.shape({
        analyticsKey: PropTypes.string,
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
        })
    }),
    fetchAnalytics: PropTypes.func.isRequired
  };

  componentDidMount() {
    const channel = 1;
    const { entityType, entityId } = this.props.entity;

    const keys = _.map(verbs[entityType], (title, verb) => {
      const key = `track.${channel}.${entityType}.${entityId}.${verb}`;
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
    this.props.fetchAnalytics(keys, from, to, sizeSec, stepSec);
  }

  @autobind
  legend(label, color, x, y) {
    return (
      <VictoryLabel
        x={x}
        y={y}
        style={{fontSize: 8, fill: `${color}`}}
        text={label}
      />
    );
  }

  @autobind
  newChart(allData) {
    const title = { x: 25, y: 12, size: 12, text: 'Last 60 Minutes' };
    return (
      <VictoryChart
        domainPadding={20}>
        <VictoryLabel
          x={title.x}
          y={title.y}
          style={{fontSize: title.size}}
          text={title.text}
        />
        {_.map(allData, (dataSet, i) => {
          return this.legend(dataSet.name, colors[i], title.x, title.y + (i + 1) * 8);
        })}
        <VictoryAxis
          label="Time"
          scale="time"
          tickFormat={(unixTimestamp) => {
            return moment(unixTimestamp).format('h:mm A');
          }}/>
        <VictoryAxis
          dependentAxis
          label="Count"
          tickFormat={(count) => (count)}/>
        {
          _.map(allData, (dataSet, i) => {
            const values = dataSet.values;
            const color = colors[i];

            _.map(values, (value) => {
              value.label = value.y;
            });

            return (
              <VictoryGroup
                data={values}>
                <VictoryScatter
                  labelComponent={
                    <VictoryTooltip
                      cornerRadius={0}
                      pointerLength={0}
                      style={{fill: color}}
                      flyoutStyle={{stroke: color}}
                      labels={(value) => (value.y)}
                    />
                  }
                  style={{
                    data: { fill: color, opacity: (value) => value.y == 0 ? 0 : 1 }
                  }}
                  x="x"
                  y="y"/>
                <VictoryLine
                  style={{
                    data: { stroke: color }
                  }}/>
              </VictoryGroup>
            );
          })
        }
      </VictoryChart>
    );
  }

  get content() {
    const { analytics } = this.props;

    if (analytics.isFetching === false) {
      if (!analytics.err) {

        const data = _.map(analytics.values, (rawValues, k) => {
          let values = _.map(rawValues, (v) => {
            return {
              x: new Date(v.x * 1000),
              y: v.y
            };
          });

          const verb = _.find(analytics.keys, {'key': k});

          return {
            name: verb.title,
            values: values,
            strokeWidth: verb.strokeWidth,
            strokeDashArray: verb.strokeDashArray,
          };
        });

        return this.newChart(data);
      } else {
        return <ErrorAlerts error={analytics.err} />;
      }
    } else {
      return <WaitAnimation />;
    }
  }

  render() {
    return (
      <div styleName="analytics-page-container">
        <div styleName="analytics-page-questions">
          <QuestionBox
            title="Total Revenue"
            content={<Currency value="578657" />}
            footer={<TrendButton trendType={TrendType.gain} value={90}/>}
          />
          <QuestionBox
            title="Total Orders"
            content={132}
            footer={<TrendButton trendType={TrendType.loss} value={10}/>}
          />
          <QuestionBox
            title="Avg. Num. Per Order"
            content={1}
            footer={<TrendButton trendType={TrendType.steady} value={0}/>}
          />
          <QuestionBox
            title="Total In Carts"
            content={132}
            footer={<TrendButton trendType={TrendType.loss} value={10}/>}
          />
          <QuestionBox
            title="Product Conversion"
            content={'7.2%'}
            footer={<TrendButton trendType={TrendType.gain} value={3}/>}
          />
        </div>
        {this.content}
      </div>
    );
  }
}
