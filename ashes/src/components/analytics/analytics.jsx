// libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
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
import ProductConversionChart from './charts/product-conversion-chart';
import TotalRevenueChart, { ChartSegmentType } from './charts/total-revenue-chart';
import SegmentControlList from './segment-control-list';
import { Props as SegmentControlType } from './segment-control';

// styles
import styles from './analytics.css';

// redux
import * as AnalyticsActions from '../../modules/analytics';

// types
type State = {
  dateRangeBegin: string, // Unix Timestamp
  dateRangeEnd: string, // Unix Timestamp
  dateDisplay: string,
  question: QuestionBoxType,
  segment: SegmentControlType,
}

const sourceDropdownColumns = [
  { field: 'google', text: 'Google' },
  { field: 'facebook', text: 'Facebook' },
  { field: 'email', text: 'Email' },
  { field: 'direct', text: 'Direct' },
];

const questionTitles = {
  TotalRevenue: 'Total Revenue',
  TotalOrders: 'Total Orders',
  AverageNumberPerOrder: 'Avg. Num. Per Order',
  TotalInCarts: 'Total In Carts',
  ProductConversionRate: 'Product Conversion',
};

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
const unixTimes = {
  hour: 7200,
  day: 86400,
  week: 604800,
  month: 2628000, // 1 month is about 730 hours
};

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
      chartValues: PropTypes.object,
      stats: PropTypes.object,
      from: PropTypes.number,
      to: PropTypes.number,
      sizeSec: PropTypes.number,
      stepSec: PropTypes.number,
      err: PropTypes.any,
      isFetching: PropTypes.bool,
      isFetchingStats: PropTypes.bool,
      route: PropTypes.shape({
        action: PropTypes.string,
        idKey: PropTypes.string,
      })
    }),
    fetchAnalytics: PropTypes.func.isRequired,
    questionBoxes: PropTypes.array,
  };

  static defaultProps = {
    questionBoxes: [
      {
        id: 'TotalRevenue',
        title: questionTitles.TotalRevenue,
        content: <Currency value="0" />,
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
      },
      {
        id: 'TotalOrders',
        title: questionTitles.TotalOrders,
        content: 0,
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
      },
      {
        id: 'AverageNumberPerOrder',
        title: questionTitles.AverageNumberPerOrder,
        content: 0,
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
      },
      {
        id: 'TotalInCarts',
        title: questionTitles.TotalInCarts,
        content: 0,
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
      },
      {
        id: 'ProductConversionRate',
        title: questionTitles.ProductConversionRate,
        content: '0%',
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
      },
    ],
    segments: [
      { id: 0, title: 'Day', isActive: true },
      { id: 1, title: 'Week' },
      { id: 2, title: 'Month' },
    ],
  };

  state: State = {
    dateRangeBegin: moment().startOf('day').unix(),
    dateRangeEnd: moment().unix(),
    dateDisplay: moment().format(datePickerFormat),
    question: null,
    segment: null,
  };

  componentDidMount() {
    this.props.fetchProductStats(this.props.entity.entityId);
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
    const { segments, entity } = this.props;
    const { dateRangeBegin, dateRangeEnd } = this.state;

    switch(question.title) {
      case questionTitles.ProductConversionRate:
        this.setState({question: question});
        this.props.fetchProductConversion(entity.entityId);
        break;
      case questionTitles.TotalRevenue:
        this.setState({question: question, segment: _.head(segments)});
        this.props.fetchProductTotalRevenue(dateRangeBegin, dateRangeEnd, entity.entityId, unixTimes.hour);
        break;
    }
  }

  @autobind
  onSegmentControlSelect(segment) {
    this.setState({segment: segment});
  }

  @autobind
  setQuestionBoxesFromStats(questionBoxes, stats) {

    if (!_.isEmpty(stats)) {
      _.map(questionBoxes, (qb) => {
        const productValue = stats[qb.id];
        const avgValue = stats[`Average${qb.id}`];

        let trendValue = null;
        let trend = TrendType.steady;

        if (avgValue === 0) {
          trendValue = 0;
        } else {
          trendValue = _.round(((productValue - avgValue) / avgValue) * 100, 0);
          trend = (trendValue > 0) ? TrendType.gain : TrendType.loss;
        }

        qb.footer = (
          <TrendButton
            trendType={trend}
            value={Math.abs(trendValue)}
            />
        );

        switch (qb.title) {
          case questionTitles.TotalRevenue:
            qb.content = <Currency value={productValue.toString()} />;
            break;
          case questionTitles.TotalOrders:
          case questionTitles.AverageNumberPerOrder:
          case questionTitles.TotalInCarts:
            qb.content = productValue.toString();
            break;
          case questionTitles.ProductConversionRate:
            qb.content = `${_.round(productValue, 2)}%`;
            break;
        }
      });
    }
  }

  get dateDisplay() {
    return this.state.dateDisplay;
  }

  get question() {
    return this.state.question;
  }

  get segment() {
    return this.state.segment;
  }

  get chartFromQuestion() {
    if (_.isNull(this.question)) {
      return false;
    }

    const { analytics, segments } = this.props;

    if (!analytics.isFetching) {
      switch (this.question.title) {
        case questionTitles.ProductConversionRate:
          return <ProductConversionChart jsonData={analytics.chartValues}/>;
        case questionTitles.TotalRevenue:
          return(
            <div>
              <SegmentControlList
                items={segments}
                onSelect={this.onSegmentControlSelect}
                activeSegment={this.segment}
                />
              <TotalRevenueChart
                jsonData={analytics.chartValues} 
                queryKey={analytics.keys}
                segmentType={ChartSegmentType.Day}
                debugMode={false}
                />
            </div>
          );
        default:
          return false;
      }
    } else {
      return false;
    }
  }

  get productStats() {
    const { analytics, questionBoxes } = this.props;

    this.setQuestionBoxesFromStats(questionBoxes, analytics.stats);

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
            identifier={'analytics-source-filter'}
            />
        </div>
        <div styleName="analytics-page-questions">
          <QuestionBoxList
            onSelect={this.onQuestionBoxSelect}
            items={questionBoxes}
            activeQuestion={this.question}
            />
        </div>
      </div>
    );
  }

  get filterHeaders() {
    const { analytics } = this.props;

    if (!analytics.isFetchingStats) {
      if (!analytics.err) {
        return this.productStats;
      } else {
        return <ErrorAlerts error={analytics.err} />;
      }
    } else {
      return <WaitAnimation />;
    }
  }

  get content() {
    const { analytics } = this.props;

    if (!analytics.isFetching && !_.isNull(this.question)) {
      if (!analytics.err) {
        return this.chartFromQuestion;
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
