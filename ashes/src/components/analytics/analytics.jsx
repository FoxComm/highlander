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
import QuestionBoxList from './question-box-list';
import { Props as QuestionBoxType } from './question-box';
import Currency from '../common/currency';
import TrendButton, { TrendType } from './trend-button';
import StaticColumnSelector from './static-column-selector';
import { Dropdown } from '../dropdown';

// styles
import styles from './analytics.css';

// redux
import * as AnalyticsActions from '../../modules/analytics';

// types
type State = {
  dateRangeBegin: string, // Unix Timestamp
  dateRangeEnd: string, // Unix Timestamp
  dateDisplay: string,
  question: any,
}

const verbs = {
  product: {
    list: 'Shown in Category',
    pdp: 'Viewed Pdp',
    cart: 'Added To Cart',
  }
}; 

const colors = ['#2ca02c', '#ff7f0e', '#662ca0'];

const sourceDropdownColumns = [
  { field: 'google', text: 'Google' },
  { field: 'facebook', text: 'Facebook' },
  { field: 'email', text: 'Email' },
  { field: 'direct', text: 'Direct' },
];

const questions: Array<QuestionBoxType> = [
  {
    title: 'Total Revenue',
    content: <Currency value="578657" />,
    footer: <TrendButton trendType={TrendType.gain} value={90}/>,
  },
  {
    title: 'Total Orders',
    content: 132,
    footer: <TrendButton trendType={TrendType.loss} value={10}/>,
  },
  {
    title: 'Avg. Num. Per Order',
    content: 1,
    footer: <TrendButton trendType={TrendType.steady} value={0}/>,
  },
  {
    title: 'Total In Carts',
    content: 132,
    footer: <TrendButton trendType={TrendType.loss} value={10}/>,
  },
  {
    title: 'Product Conversion',
    content: '7.2%',
    footer: <TrendButton trendType={TrendType.gain} value={3}/>,
  },
];

const datePickerType = {
  Today: 0,
  Yesterday: 1,
  LastWeek: 2,
  Last30: 3,
  Last90: 4,
  Range: 5,
};
const datePickerOptions = [
  { id: datePickerType.Today, displayText: 'Today'},
  { id: datePickerType.Yesterday, displayText: 'Yesterday'},
  { id: datePickerType.LastWeek, displayText: 'Last Week'},
  { id: datePickerType.Last30, displayText: 'Last 30 Days'},
  { id: datePickerType.Last90, displayText: 'Last 90 Days'},
];
const datePickerFormat = 'MM/DD/YYYY';

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

  state: State = {
    dateRangeBegin: moment().startOf('day').unix(),
    dateRangeEnd: moment().unix(),
    dateDisplay: moment().format(datePickerFormat),
    question: null,
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

  @autobind
  onDatePickerChange(selectionIndex) {
    let displayText = '';
    let endDisplayText = '';
    let beginDisplayText = '';

    let newDateRangeBegin = null;
    let newDateRangeEnd = null;

    const setDisplayTexts = function(previousDays) {
      newDateRangeBegin = moment().subtract(previousDays, 'days').unix();
      newDateRangeEnd = moment().unix();

      beginDisplayText = moment().subtract(previousDays, 'days').format(datePickerFormat);
      endDisplayText = moment().format(datePickerFormat);

      displayText = `${beginDisplayText} - ${endDisplayText}`;
    };

    switch(selectionIndex) {
      case datePickerType.Today:
        newDateRangeBegin = moment().startOf('day').unix();
        newDateRangeEnd = moment().unix();

        displayText = `${moment().format(datePickerFormat)}`;
        break;
      case datePickerType.Yesterday:
        setDisplayTexts(1);
        break;
      case datePickerType.LastWeek:
        setDisplayTexts(7);
        break;
      case datePickerType.Last30:
        setDisplayTexts(30);
        break;
      case datePickerType.Last90:
        setDisplayTexts(90);
        break;
      default:
        console.log('INVALID DATE RANGE');
        displayText = moment().format(datePickerFormat);
        break;
    }

    this.setState({
      dateDisplay: displayText,
      dateRangeBegin: newDateRangeBegin,
      dateRangeEnd: newDateRangeEnd,
    });
  }

  @autobind
  onQuestionBoxSelect(question) {
    this.setState({question: question});
  }

  get dateDisplay() {
    return this.state.dateDisplay;
  }

  get question() {
    return this.state.question;
  }

  get filterHeaders() {
    return (
      <div>
        <div styleName="analytics-filters">
          <Dropdown
            styleName="analytics-filter-date-picker"
            name="dateControl"
            items={_.map(datePickerOptions, ({id, displayText}) => [id, displayText])}
            placeholder={`${moment().format(datePickerFormat)}`}
            changeable={true}
            onChange={this.onDatePickerChange}
            value={this.dateDisplay}
            renderNullTitle={(value, placeholder) => {
              return _.isNull(value) ? placeholder : value;
            }}
          />
          <StaticColumnSelector
            setColumns={null}
            columns={sourceDropdownColumns}
            actionButtonText="Apply"
            dropdownTitle="Sources"
            identifier={'analytics-source-filter'} />
        </div>
        <div styleName="analytics-page-questions">
          <QuestionBoxList
            onSelect={this.onQuestionBoxSelect}
            items={questions}
            activeQuestion={this.question}
          />
        </div>
      </div>
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
        {this.filterHeaders}
        {this.content}
      </div>
    );
  }
}
