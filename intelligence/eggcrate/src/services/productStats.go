package services

import (
	"encoding/json"
	"net/http"

	"github.com/FoxComm/highlander/intelligence/eggcrate/src/responses"
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/util"

	"github.com/labstack/echo"
	"strconv"
	"time"
)

func GetProductStats(c echo.Context) error {
	id := c.Param("id")
	channel := c.Param("channel")

	from, to, frequency := c.QueryParam("from"), c.QueryParam("to"), c.QueryParam("frequency")
	resp, err := henhouseProductStatsWithFrequency(id, channel, from, to, frequency)
	if err != nil {
		return c.String(http.StatusBadRequest, err.Error())
	}
	return c.String(http.StatusOK, resp)
}

func productStatKeys(id string, channel string) []string {
	all := "track." + channel + ".product."
	single := all + id + "."
	return []string{
		single + "revenue",
		single + "purchase",
		single + "cart",
		single + "list",
		single + "pdp",
		all + "revenue",
		all + "purchase",
		all + "cart",
		all + "list",
		all + "pdp",
	}
}

func frequencyToTimeSlices(a, b, frequency string) ([]int64, error) {
	var unixFrom = int64(0)
	var err error = nil
	if a != "" {
		unixFrom, err = strconv.ParseInt(a, 10, 0)
		if err != nil {
			return nil, err
		}
	}

	var unixTo = time.Now().Unix()
	if b != "" {
		unixTo, err = strconv.ParseInt(b, 10, 0)
		if err != nil {
			return nil, err
		}
	}
	if frequency == "" {
		return []int64{unixFrom, unixTo}, nil
	} else {
		freq, err := util.ParseFrequency(frequency)
		if err != nil {
			return nil, err
		}
		return util.SliceRangeToUnixTime(freq, time.Unix(unixFrom, 0), time.Unix(unixTo, 0)), nil
	}
}

func henhouseProductStatsWithFrequency(id, channel, a, b string, frequency string) (string, error) {
	times, err := frequencyToTimeSlices(a, b, frequency)
	if err != nil {
		return "", err
	}
	payload, err := json.Marshal(times)
	if err != nil {
		return "", err
	}

	keys := append(productStatKeys(id, channel), "track."+channel+".product.activated")
	valuesResponse, qErr := util.HenhouseValuesQuery(keys, payload)
	if qErr != nil {
		return "", qErr
	}

	activeKey := []string{"track." + channel + ".product.activated"}
	activeResponse, qErr := util.HenhouseQuery("diff", activeKey, "0", strconv.FormatInt(times[0], 10), "")
	if qErr != nil {
		return "", qErr
	}

	activeProducts := responses.GetSum("product.activated", activeResponse)

	responseData := readHenhouseValues(valuesResponse, channel, id, activeProducts)
	result := make([]responses.ProductStatsResponseWithTime, len(responseData), len(responseData))

	for i, item := range responseData {
		result[i] = buildResponseItem(item)
	}

	if frequency == "" {
		out, err := json.Marshal(&result[0].Stats)
		return string(out), err
	} else {
		out, err := json.Marshal(&result)
		return string(out), err
	}
}

type henhouseValues struct {
	pdpViews    int
	listed      int
	addedToCart int
	ordered     int
	revenue     int
}

type henhouseResponseData struct {
	time           int64
	single         henhouseValues
	all            henhouseValues
	activeProducts int
}

func readHenhouseValues(values map[string]responses.ValuePairs, channel string, productId string, activeProductsBefore int) []henhouseResponseData {
	// HenhouseValuesQuery returns map of arrays (data grouped by key).
	// But for further processing we need array of maps (grouping by time).
	all := "track." + channel + ".product."
	single := all + productId + "."

	var length *int = nil

	//find min length of values among all keys.
	for _, v := range values {
		if length == nil {
			var l = len(v)
			length = &l
		} else {
			if len(v) < *length {
				*length = len(v)
			}
		}
	}

	active := activeProductsBefore
	result := make([]henhouseResponseData, 0, *length)

	for index := 0; index < *length; index++ {
		data := henhouseResponseData{}
		data.time = values[single+"revenue"][index].X

		data.single.revenue = values[single+"revenue"][index].Y
		data.single.ordered = values[single+"purchase"][index].Y
		data.single.addedToCart = values[single+"cart"][index].Y
		data.single.listed = values[single+"list"][index].Y
		data.single.pdpViews = values[single+"pdp"][index].Y

		data.all.revenue = values[all+"revenue"][index].Y
		data.all.ordered = values[all+"purchase"][index].Y
		data.all.addedToCart = values[all+"cart"][index].Y
		data.all.listed = values[all+"list"][index].Y
		data.all.pdpViews = values[all+"pdp"][index].Y

		data.activeProducts = active + values[all+"activated"][index].Y

		active = data.activeProducts

		result = append(result, data)
	}
	return result
}

func buildResponseItem(data henhouseResponseData) responses.ProductStatsResponseWithTime {
	inCart := data.single.addedToCart - int(data.single.ordered)

	conversionRate := 0.0
	if data.single.listed > 0 {
		conversionRate = float64(data.single.ordered) / float64(data.single.listed)
	}

	allInCart := data.all.addedToCart - data.all.ordered

	allConversionRate := 0.0
	if data.all.listed > 0 {
		allConversionRate = float64(data.all.ordered) / float64(data.all.listed)
	}

	avgRevenue := 0.0
	avgOrdered := 0.0
	avgInCart := 0.0
	avgPdpViews := 0.0
	avgConversionRate := 0.0

	//stores the count of all products active
	if data.activeProducts > 0 {
		flActive := float64(data.activeProducts)
		avgRevenue = float64(data.all.revenue) / flActive
		avgOrdered = float64(data.all.ordered) / flActive
		avgInCart = float64(allInCart) / flActive
		avgPdpViews = float64(data.all.pdpViews) / flActive
		avgConversionRate = allConversionRate / flActive
	}

	average := &responses.AverageProductStatsResponse{
		TotalRevenue:          avgRevenue,
		TotalOrders:           avgOrdered,
		TotalPdPViews:         avgPdpViews,
		TotalInCarts:          avgInCart,
		ProductConversionRate: avgConversionRate,
	}

	resp := responses.ProductStatsResponse{
		TotalRevenue:          data.single.revenue,
		TotalOrders:           data.single.ordered,
		TotalPdPViews:         data.single.pdpViews,
		TotalInCarts:          inCart,
		ProductConversionRate: conversionRate,
		Average:               average,
		ActiveProducts:        data.activeProducts,
	}
	return responses.ProductStatsResponseWithTime{Time: data.time, Stats: resp}
}
