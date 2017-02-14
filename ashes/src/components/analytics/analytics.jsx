// @flow

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
import type { Props as QuestionBoxType } from './question-box';
import Currency from '../common/currency';
import TrendButton, { TrendType } from './trend-button';
import StaticColumnSelector from './static-column-selector';
import { Dropdown } from '../dropdown';
import ProductConversionChart from './charts/product-conversion-chart';
import TotalRevenueChart, { ChartSegmentType } from './charts/total-revenue-chart';
import SegmentControlList from './segment-control-list';
import type { Props as SegmentControlType } from './segment-control';

const ActionBlock = (props) => {
  const { onActionClick, style } = props;

  const xIconSvg = (
    <svg width="14px" height="14px" viewBox="0 0 14 14" style={style}>
      <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
        <g transform="translate(-485.000000, -557.000000)" fill="#000000">
          <g transform="translate(485.000000, 557.000000)">
            <g>
              <g>
                <path d="M8.66317371,6.99940845 L13.6542723,2.00830986 C14.1137089,1.54887324 14.1137089,0.803981221 13.6542723,0.344610329 C13.1948357,-0.114826291 12.4500094,-0.114826291 11.9905728,0.344610329 L6.99940845,5.33577465 L2.00824413,0.344544601 C1.54880751,-0.114892019 0.803981221,-0.114892019 0.344544601,0.344544601 C-0.114826291,0.803981221 -0.114826291,1.54887324 0.344544601,2.00824413 L5.33570892,6.99934272 L0.344544601,11.990507 C-0.114826291,12.4499437 -0.114826291,13.1948357 0.344544601,13.6542066 C0.803981221,14.1136432 1.54880751,14.1136432 2.00824413,13.6542066 L6.99940845,8.66304225 L11.9905728,13.6542066 C12.4499437,14.1136432 13.1948357,14.1136432 13.6542723,13.6542066 C14.1137089,13.19477 14.1137089,12.4499437 13.6542723,11.990507 L8.66317371,6.99940845 Z" id="Shape"></path>
              </g>
            </g>
          </g>
        </g>
      </g>
    </svg>
  );

  return (
    <a
      className='fc-modal-close'
      onClick={onActionClick}
    >
      {xIconSvg}
    </a>
  );
};

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
  dataFetchTimeSize: number,
  comparisonPeriod: {
    dateDisplay: string,
    dateRangeBegin: string,
    dateRangeEnd: string,
    dataFetchTimeSize: number,
  },
}

type Props = {
  entity: {
    entityId: string|number,
    entityType: string,
  },
  analytics: {
    analyticsKey: string,
    chartValues: mixed,
    stats: any,
    from: number,
    to: number,
    sizeSec: number,
    stepSec: number,
    err: mixed,
    isFetching: boolean,
    isFetchingStats: boolean,
    route: {
      action: string,
      idKey: string,
    },
  },
  questionBoxes: Array<QuestionBoxType>,
  segments: Array<SegmentControlType>, 
}

// consts
const sourceDropdownColumns = [
  { field: 'google', text: 'Google' },
  { field: 'facebook', text: 'Facebook' },
  { field: 'email', text: 'Email' },
  { field: 'direct', text: 'Direct' },
];

const questionTitles = {
  TotalRevenue: 'Total Revenue',
  TotalOrders: 'Total Orders',
  TotalPdPViews: 'Total PDP Views',
  TotalInCarts: 'Total In Carts',
  ProductConversionRate: 'Product Conversion',
};

const segmentTitles = {
  day: 'Day',
  week: 'Week',
  month: 'Month',
};

const unixTimes = {
  twoHour: 7200,
  day: 86400,
  week: 604800,
  month: 2628000, // 1 month is about 730 hours
};

const datePickerType = {
  Today: 0,
  Yesterday: 1,
  LastWeek: 2,
  Last30: 3,
  Last90: 4,
  Range: 5, //TODO: Implement DatePicker for custom date ranges
};
const datePickerOptions = [
  { id: datePickerType.Today, displayText: 'Today'},
  { id: datePickerType.Yesterday, displayText: 'Yesterday'},
  { id: datePickerType.LastWeek, displayText: 'Last Week'},
  { id: datePickerType.Last30, displayText: 'Last 30 Days'},
  { id: datePickerType.Last90, displayText: 'Last 90 Days'},
];
const datePickerFormat = 'MM/DD/YYYY';

const comparisonPeriodOptions = [
  { id: datePickerType.Today, displayText: 'Today'},
  { id: datePickerType.Yesterday, displayText: 'Yesterday'},
  { id: datePickerType.LastWeek, displayText: 'Last Week'},
  { id: datePickerType.Last30, displayText: 'Last 30 Days'},
  { id: datePickerType.Last90, displayText: 'Last 90 Days'},
];

// helpers
export function
percentDifferenceFromAvg(percentValue: number, avgPercentValue: number): number {
  if (avgPercentValue === 0) return 0;
  return _.round(((percentValue - avgPercentValue) / avgPercentValue) * 100, 0);
};

@connect((state, props) => ({analytics: state.analytics}), AnalyticsActions)
export default class Analytics extends React.Component {

  static defaultProps: { questionBoxes: Array<QuestionBoxType>, segments: Array<SegmentControlType> } = {
    questionBoxes: [
      {
        id: 'TotalRevenue',
        title: questionTitles.TotalRevenue,
        content: <Currency value="0" />,
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
        isActive: true,
        onClick: _.noop,
      },
      {
        id: 'TotalOrders',
        title: questionTitles.TotalOrders,
        content: 0,
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
        onClick: _.noop,
      },
      {
        id: 'TotalPdPViews',
        title: questionTitles.TotalPdPViews,
        content: 0,
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
        onClick: _.noop,
      },
      {
        id: 'TotalInCarts',
        title: questionTitles.TotalInCarts,
        content: 0,
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
        onClick: _.noop,
      },
      {
        id: 'ProductConversionRate',
        title: questionTitles.ProductConversionRate,
        content: '0%',
        footer: <TrendButton trendType={TrendType.steady} value={0} />,
        onClick: _.noop,
      },
    ],
    segments: [
      { 
        id: 0, 
        title: segmentTitles.day, 
        onClick: _.noop,
        isActive: true 
      },
      { 
        id: 1, 
        title: segmentTitles.week,
        onClick: _.noop,
      },
      { 
        id: 2, 
        title: segmentTitles.month, 
        onClick: _.noop,
      },
    ],
  };

  state: State = {
    dateRangeBegin: moment().startOf('day').unix(),
    dateRangeEnd: moment().unix(),
    dateDisplay: moment().format(datePickerFormat),
    question: _.noop,
    segment: _.noop,
    dataFetchTimeSize: 0,
    comparisonPeriod: {
      dateDisplay: 'Comparison Period',
      dateRangeBegin: moment().startOf('day').unix(),
      dateRangeEnd: moment().unix(),
      dataFetchTimeSize: 0,
    },
  };

  constructor(props: Props) {
    super(props);
    this.state.question = _.head(props.questionBoxes);
    this.state.segment = _.head(props.segments);
    this.state.dataFetchTimeSize = unixTimes.twoHour; 
  }

  componentDidMount() {
    this.props.fetchProductStats(this.props.entity.entityId);
  }

  componentWillUnmount() {
    this.props.resetAnalyticsValues();
  }

  @autobind
  fetchData(
    question: QuestionBoxType,
    dateRangeBegin: string,
    dateRangeEnd: string,
    dataFetchTimeSize: number
  ) {
    if (_.isNil(question)) {
      return;
    }

    const { segments, entity } = this.props;

    switch(question.title) {
      case questionTitles.TotalRevenue:
        this.props.fetchProductTotalRevenue(dateRangeBegin, dateRangeEnd, entity.entityId, dataFetchTimeSize);
        break;
      case questionTitles.TotalOrders:
        this.props.fetchProductTotalOrders(dateRangeBegin, dateRangeEnd, entity.entityId, dataFetchTimeSize);
        break;
      case questionTitles.TotalPdPViews:
        this.props.fetchProductTotalPdPViews(dateRangeBegin, dateRangeEnd, entity.entityId, dataFetchTimeSize);
        break;
      case questionTitles.TotalInCarts:
        this.props.fetchProductTotalInCarts(dateRangeBegin, dateRangeEnd, entity.entityId, dataFetchTimeSize);
        break;
      case questionTitles.ProductConversionRate:
        if(dataFetchTimeSize > 0) {
          this.props.fetchProductConversion(entity.entityId, dateRangeBegin, dateRangeEnd);
        } else {
          this.props.fetchProductConversion(entity.entityId);
        }
        break;
    }
  }

  @autobind
  onDateDropdownChange(selectionIndex: number) {
    const { segment, dataFetchTimeSize } = this.state;

    let displayText = '';
    let endDisplayText = '';
    let beginDisplayText = '';

    let newDateRangeBegin = '';
    let newDateRangeEnd = '';
    let newDataFetchTimeSize = dataFetchTimeSize;

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
        newDataFetchTimeSize = unixTimes.twoHour;
        break;
      case datePickerType.Yesterday:
        setDisplayTexts(1);
        newDataFetchTimeSize = unixTimes.day;
        break;
      case datePickerType.LastWeek:
        setDisplayTexts(7);
        newDataFetchTimeSize = unixTimes.day;
        break;
      case datePickerType.Last30:
        setDisplayTexts(30);
        newDataFetchTimeSize = unixTimes.week;
        break;
      case datePickerType.Last90:
        setDisplayTexts(90);
        newDataFetchTimeSize = unixTimes.month;
        break;
      default:
        console.log('INVALID DATE RANGE');
        displayText = moment().format(datePickerFormat);
        newDataFetchTimeSize = unixTimes.twoHour;
        break;
    }

    // TODO: Redo this logic, datePickerType.Today is a special case
    if (datePickerType.Today !== selectionIndex) {
      switch (segment.title) {
        case segmentTitles.day:
          newDataFetchTimeSize = unixTimes.day;
          break;
        case segmentTitles.week:
          newDataFetchTimeSize = unixTimes.week;
          break;
        case segmentTitles.month:
          newDataFetchTimeSize = unixTimes.month;
          break;
      }
    }

    return {
      displayText: displayText,
      newDateRangeBegin: newDateRangeBegin,
      newDateRangeEnd: newDateRangeEnd,
      newDataFetchTimeSize: newDataFetchTimeSize
    };
  }

  @autobind
  onDatePickerChange(selectionIndex: number) {
    const { question } = this.state;
    const { displayText, newDateRangeBegin,
      newDateRangeEnd, newDataFetchTimeSize } = this.onDateDropdownChange(selectionIndex);

    this.setState({
        dateDisplay: displayText,
        dateRangeBegin: newDateRangeBegin,
        dateRangeEnd: newDateRangeEnd,
        dataFetchTimeSize: newDataFetchTimeSize,
      }
    );

    if (question.title !== questionTitles.ProductConversionRate) {
      this.fetchData(question, newDateRangeBegin, newDateRangeEnd, newDataFetchTimeSize);
    }
  }

  @autobind
  onComparisonPeriodChange(selectionIndex: number) {
    const { question } = this.state;
    const { displayText, newDateRangeBegin,
      newDateRangeEnd, newDataFetchTimeSize } = this.onDateDropdownChange(selectionIndex);

    this.setState({
      comparisonPeriod: {
        dateDisplay: displayText,
        dateRangeBegin: newDateRangeBegin,
        dateRangeEnd: newDateRangeEnd,
        dataFetchTimeSize: newDataFetchTimeSize,
      }
    },
      this.fetchData(question, newDateRangeBegin, newDateRangeEnd, newDataFetchTimeSize)
    );
  }

  @autobind
  onRemoveComparison() {
    const { question } = this.state;

    const newDateRangeBegin = moment().startOf('day').unix();
    const newDateRangeEnd = moment().unix();
    const newDataFetchTimeSize = 0;

    this.setState({
      comparisonPeriod: {
        dateDisplay: 'Comparison Period',
        dateRangeBegin: newDateRangeBegin,
        dateRangeEnd: newDateRangeEnd,
        dataFetchTimeSize: newDataFetchTimeSize,
      }
    },
      this.fetchData(question, newDateRangeBegin, newDateRangeEnd, newDataFetchTimeSize)
    );
  }

  @autobind
  onQuestionBoxSelect(question: QuestionBoxType) {
    switch(question.title) {
      case questionTitles.TotalRevenue:
      case questionTitles.TotalOrders:
      case questionTitles.TotalPdPViews:
      case questionTitles.TotalInCarts:
        const { dateRangeBegin, dateRangeEnd, dataFetchTimeSize } = this.state;

        this.setState({ question: question },
          this.fetchData(question, dateRangeBegin, dateRangeEnd, dataFetchTimeSize)
        );
        break;
      case questionTitles.ProductConversionRate:
        const { comparisonPeriod } = this.state;

        this.setState({
          question: question
        },
          this.fetchData(
            question,
            comparisonPeriod.dateRangeBegin,
            comparisonPeriod.dateRangeEnd,
            comparisonPeriod.dataFetchTimeSize
          )
        );
        break;
    }
  }

  //TODO: Work out the size and step henhouse logic
  @autobind
  onSegmentControlSelect(segment: SegmentControlType) {
    const { question, dateRangeBegin, dateRangeEnd, dataFetchTimeSize } = this.state;

    let newDataFetchTimeSize = dataFetchTimeSize;

    switch(segment.title) {
      case segmentTitles.day:
        newDataFetchTimeSize = (dateRangeBegin === moment().startOf('day').unix())
          ? unixTimes.twoHour
          : unixTimes.day;
      break;
      case segmentTitles.week:
        newDataFetchTimeSize = unixTimes.week;
      break;
      case segmentTitles.month:
        newDataFetchTimeSize = unixTimes.month;
      break;
    }

    this.setState({
      segment: segment,
      dataFetchTimeSize: newDataFetchTimeSize,
    }, this.fetchData(question, dateRangeBegin, dateRangeEnd, newDataFetchTimeSize));
  }

  @autobind
  setQuestionBoxesFromStats(questionBoxes: Array<QuestionBoxType>, stats: any) {

    if (!_.isEmpty(stats)) {
      _.map(questionBoxes, (qb) => {
        const productValue = stats[qb.id];
        const avgValue = stats.Average[qb.id];

        // set QuestionBox Trends
        let trendValue = 0;
        let trend = TrendType.steady;

        trendValue = percentDifferenceFromAvg(productValue, avgValue);
        if (trendValue > 0) {
          trend = TrendType.gain;
        } else if (trendValue < 0) {
          trend = TrendType.loss;
        }

        qb.footer = (
          <TrendButton
            trendType={trend}
            value={Math.abs(trendValue)}
            />
        );

        // set QuestionBox Content
        switch (qb.title) {
          case questionTitles.TotalRevenue:
            qb.content = <Currency value={productValue.toString()} />;
            break;
          case questionTitles.TotalOrders:
          case questionTitles.TotalPdPViews:
          case questionTitles.TotalInCarts:
            qb.content = productValue.toString();
            break;
          case questionTitles.ProductConversionRate:
            qb.content = `${_.round(productValue, 2)}%`;
            break;
        }

        // Prevent Reconciliation errors from excessive chart rendering
        // @see https://facebook.github.io/react/docs/reconciliation.html
        qb.isClickable = !this.props.analytics.isFetching;
      });
    }
  }

  get chartSegmentType(): string {
    const { dataFetchTimeSize } = this.state;

    switch(dataFetchTimeSize) {
      case unixTimes.twoHour:
        return ChartSegmentType.Hour;
      case unixTimes.day:
        return ChartSegmentType.Day;
      case unixTimes.week:
        return ChartSegmentType.Week;
      case unixTimes.month:
        return ChartSegmentType.Month;
      default:
        return ChartSegmentType.Day;
    }
  }

  get chartFromQuestion() {
    const { question, dataFetchTimeSize, segment, comparisonPeriod } = this.state;

    if (_.isNil(question)) {
      return false;
    }

    const { analytics, segments } = this.props;

    if (!_.isNil(analytics.isFetching) && !analytics.isFetching) {
      const segmentCtrlList = (
        <SegmentControlList
          items={segments}
          onSelect={this.onSegmentControlSelect}
          activeSegment={segment}
        />
      );

      switch (question.title) {
        case questionTitles.TotalRevenue:
          return(
            <div>
              { segmentCtrlList }
              <TotalRevenueChart
                jsonData={analytics.chartValues} 
                queryKey={analytics.keys}
                segmentType={this.chartSegmentType}
                currencyCode="USD"
                />
            </div>
          );
        case questionTitles.TotalOrders:
        case questionTitles.TotalPdPViews:
        case questionTitles.TotalInCarts:
          return (
            <div>
              {segmentCtrlList}
              <TotalRevenueChart
                jsonData={analytics.chartValues}
                queryKey={analytics.keys}
                segmentType={this.chartSegmentType}
                />
            </div>
          );
        case questionTitles.ProductConversionRate:
          const comparisonCancelButtonVisibility = comparisonPeriod.dataFetchTimeSize > 0
            ? 'visible'
            : 'hidden';

          let conversionComparison = {};
          if (_.has(analytics, 'chartValues.Comparison')) {
            conversionComparison = analytics.chartValues.Comparison;
            conversionComparison.Average = analytics.chartValues.Average;
          }

          return (
            <div>
              <Dropdown
                styleName="comparison-period-filter-date-picker"
                name="dateControl"
                items={_.map(comparisonPeriodOptions, ({id, displayText}) => [id, displayText])}
                placeholder="Comparison Period"
                changeable={true}
                onChange={this.onComparisonPeriodChange}
                value={comparisonPeriod.dateDisplay}
                renderNullTitle={(value, placeholder) => {
                  return _.isNil(value) ? placeholder : value;
                }}
              />
              <ActionBlock
                onActionClick={this.onRemoveComparison}
                style={{marginLeft: '10px', visibility: comparisonCancelButtonVisibility}}
              />
              <ProductConversionChart
                jsonData={analytics.chartValues}
                comparisonJsonData={conversionComparison}
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
    const { dateDisplay, question } = this.state;

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
            value={dateDisplay}
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
            activeQuestion={question}
            />
        </div>
      </div>
    );
  }

  get filterHeaders() {
    const { analytics } = this.props;
    const { question, dateRangeBegin, dateRangeEnd, dataFetchTimeSize } = this.state;


    if (!_.isNil(analytics.isFetchingStats) && !analytics.isFetchingStats) {
      if (!analytics.err) {
        const productStats = this.productStats;

        // Initial fetch to display the first Question
        if(_.isNil(analytics.isFetching)) {
          this.fetchData(question, dateRangeBegin, dateRangeEnd, dataFetchTimeSize);
        }

        return productStats;
      } else {
        return <ErrorAlerts error={analytics.err} />;
      }
    } else {
      return <WaitAnimation />;
    }
  }

  get content() {
    const { analytics } = this.props;
    const { question } = this.state;

    if (!_.isNil(analytics.isFetching) && !analytics.isFetching && !_.isNil(question)) {
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

