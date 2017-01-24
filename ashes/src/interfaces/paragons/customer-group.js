type Condition = Array<String>;

declare type TCustomerGroupShort = {
  id: string;
  name: string;
  type: string;
}

declare type TCustomerGroup = TCustomerGroupShort & {
  customersCount: number;
  isValid: boolean;
  stats: Object;
  conditions: Array<Condition>;
  mainCondition: string;
  elasticRequest: Object;
  createdAt: string;
  updatedAt: string;
  stats: ?Object;
};
