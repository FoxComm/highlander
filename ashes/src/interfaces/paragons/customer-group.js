type Condition = Array<String>;

declare type TCustomerGroup = {
  id: string;
  name: string;
  type: string;
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
