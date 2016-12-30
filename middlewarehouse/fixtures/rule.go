package fixtures

import "github.com/FoxComm/highlander/middlewarehouse/models/rules"

func GrandTotalRole(operator string, total int) rules.Rule {
	return rules.Rule{
		Comparison: rules.And,
		Conditions: []rules.Condition{
			rules.Condition{
				RootObject: "Order",
				Field:      "grandTotal",
				Operator:   operator,
				Value:      total,
			},
		},
	}
}

func WestCoastRole() rules.Rule {
	return rules.Rule{
		Comparison: rules.Or,
		Conditions: []rules.Condition{
			rules.Condition{
				RootObject: "ShippingAddress",
				Field:      "regionId",
				Operator:   rules.Equals,
				Value:      4129,
			},
			rules.Condition{
				RootObject: "ShippingAddress",
				Field:      "regionId",
				Operator:   rules.Equals,
				Value:      4164,
			},
			rules.Condition{
				RootObject: "ShippingAddress",
				Field:      "regionId",
				Operator:   rules.Equals,
				Value:      4177,
			},
		},
	}
}

func WestCoastAndTotalRole() rules.Rule {
	return rules.Rule{
		Comparison: rules.And,
		Rules: []rules.Rule{
			rules.Rule{
				Comparison: rules.Or,
				Conditions: []rules.Condition{
					rules.Condition{
						RootObject: "ShippingAddress",
						Field:      "regionId",
						Operator:   rules.Equals,
						Value:      4129,
					},
					rules.Condition{
						RootObject: "ShippingAddress",
						Field:      "regionId",
						Operator:   rules.Equals,
						Value:      4164,
					},
					rules.Condition{
						RootObject: "ShippingAddress",
						Field:      "regionId",
						Operator:   rules.Equals,
						Value:      4177,
					},
				},
			},
			rules.Rule{
				Comparison: rules.And,
				Conditions: []rules.Condition{
					rules.Condition{
						RootObject: "Order",
						Field:      "grandTotal",
						Operator:   rules.GreaterThanOrEquals,
						Value:      10,
					},
					rules.Condition{
						RootObject: "Order",
						Field:      "grandTotal",
						Operator:   rules.LessThan,
						Value:      100,
					},
				},
			},
		},
	}
}
