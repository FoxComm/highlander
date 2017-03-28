type Condition = Array<String>;

type TCustomerGroupShort = {
  id: number,
  name: string,
  groupType: string,
};

type TStatsUnit = {
  ordersCount: number,
  totalSales: number,
  averageOrderSize: number,
  averageOrderSum: number,
}

type TStatsPeriodUnit = {
  group: TStatsUnit,
  overall: TStatsUnit,
}

type TCustomerGroupStats = {
  day: ?TStatsPeriodUnit,
  week: ?TStatsPeriodUnit,
  month: ?TStatsPeriodUnit,
  quarter: ?TStatsPeriodUnit,
  year: ?TStatsPeriodUnit,
  overall: ?TStatsPeriodUnit,
};

type TCustomerGroup = TCustomerGroupShort & {
  customersCount: number,
  isValid: boolean,
  conditions: Array<Condition>,
  mainCondition: string,
  elasticRequest: Object,
  createdAt: string,
  updatedAt: string,
  stats: TCustomerGroupStats,
};


type TTemplate = {
  id: number,
  name: string,
  groupType: string,
  clientState: Object, // JSON with dsl request handled by UI to build group constructor
  elasticRequest: Object, // JSON with query sent to ES
};

type TTemplates = Array<TTemplate>;
