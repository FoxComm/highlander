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


declare type TTemplate = {
  id: number;
  name: string;
  clientState: Object; // JSON with dsl request handled by UI to build group constructor
  elasticRequest: Object; // JSON with query sent to ES
}

declare type TTemplates = Array<TTemplate>;
