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
import TotalRevenueChart from './charts/total-revenue-chart';
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

const questionTitles = {
  totalRevenue: 'Total Revenue',
  totalOrders: 'Total Orders',
  avgNumPerOrder: 'Avg. Num. Per Order',
  totalInCarts: 'Total In Carts',
  productConversion: 'Product Conversion',
};

const questions: Array<QuestionBoxType> = [
  {
    title: questionTitles.totalRevenue,
    content: <Currency value="578657" />,
    footer: <TrendButton trendType={TrendType.gain} value={90}/>,
  },
  {
    title: questionTitles.totalOrders,
    content: 132,
    footer: <TrendButton trendType={TrendType.loss} value={10}/>,
  },
  {
    title: questionTitles.avgNumPerOrder,
    content: 1,
    footer: <TrendButton trendType={TrendType.steady} value={0}/>,
  },
  {
    title: questionTitles.totalInCarts,
    content: 132,
    footer: <TrendButton trendType={TrendType.loss} value={10}/>,
  },
  {
    title: questionTitles.productConversion,
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
    segment: null,
  };

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

    switch(question.title) {
      case questionTitles.productConversion:
        this.props.fetchProductConversion(this.props.entity.entityId);
        break;
      case questionTitles.totalRevenue:
        this.props.fetchProductTotalRevenue();
        break;
    }
  }

  @autobind
  onSegmentControlSelect(segment) {
    this.setState({segment: segment});
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

    const { analytics } = this.props;

    if (!analytics.isFetching) {
      switch (this.question.title) {
        case questionTitles.productConversion:
          return <ProductConversionChart jsonData={analytics.values}/>;
        case questionTitles.totalRevenue:
          const segments: Array<SegmentControlType> = [
            { title: 'Day' },
            { title: 'Week' },
            { title: 'Month' },
          ];

          return(
            <div>
              <SegmentControlList
                items={segments}
                onSelect={this.onSegmentControlSelect}
                activeSegment={this.segment}
                />
              <TotalRevenueChart jsonData={{}} debugMode={true}/>
            </div>
          );
        default:
          return false;
      }
    } else {
      return false;
    }
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
