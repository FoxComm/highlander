type Condition = Array<String>;

declare type TCustomerGroup = {
  id: string;
  name: string;
  type: string;
  customersCount: number;
  isSaved: boolean;
  isValid: boolean;
  stats: Object;
  filterTerm: ?any;
  conditions: Array<Condition>;
  mainCondition: string;
  createdAt: string;
  updatedAt: string;
  stats: ?Object;
};
