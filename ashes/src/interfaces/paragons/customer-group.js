type Condition = Array<String>;

type TCustomerGroupShort = {
  id: number,
  name: string,
  type: string,
};

type TCustomerGroupStats = {
  orderCount: ?number,
  totalSales: ?number,
  averageOrderSize: ?number,
  averageorderSum: ?number,
};

type TCustomerGroup = TCustomerGroupShort & {
  stats: TCustomerGroupStats,
  customersCount: number,
  isValid: boolean,
  conditions: Array<Condition>,
  mainCondition: string,
  elasticRequest: Object,
  createdAt: string,
  updatedAt: string,
};


type TTemplate = {
  id: number,
  name: string,
  clientState: Object, // JSON with dsl request handled by UI to build group constructor
  elasticRequest: Object, // JSON with query sent to ES
};

type TTemplates = Array<TTemplate>;
