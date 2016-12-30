package fixtures

import (
	"database/sql"
	"database/sql/driver"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetAddress(id uint, regionID uint, region *models.Region) *models.Address {
	return &models.Address{
		Base: gormfox.Base{
			ID: id,
		},
		Name:        "Home address",
		RegionID:    regionID,
		Region:      *region,
		City:        "Moscow",
		Zip:         "112323",
		Address1:    "Some st, 335",
		Address2:    sql.NullString{},
		PhoneNumber: "19527352893",
	}
}

func ToAddressPayload(address *models.Address) *payloads.Address {
	return &payloads.Address{
		ID:          address.ID,
		Name:        address.Name,
		Region:      *ToRegionPayload(&address.Region),
		City:        address.City,
		Zip:         address.Zip,
		Address1:    address.Address1,
		Address2:    responses.NewStringFromSqlNullString(address.Address2),
		PhoneNumber: address.PhoneNumber,
	}
}

func GetAddressColumns() []string {
	return []string{"id", "name", "region_id", "city", "zip", "address1", "address2",
		"phone_number", "created_at", "updated_at", "deleted_at"}
}

func GetAddressRow(address *models.Address) []driver.Value {
	return []driver.Value{address.ID, address.Name, address.RegionID, address.City, address.Zip, address.Address1,
		nil, address.PhoneNumber, address.CreatedAt, address.UpdatedAt, address.DeletedAt}
}

func GetCaliforniaAddressPayload() *payloads.Address {
	return &payloads.Address{
		ID:   1,
		Name: "Donkey Hote",
		Region: payloads.Region{
			ID:          4129,
			Name:        "California",
			CountryID:   1,
			CountryName: "United States of America",
		},
		City:        "Burlingame",
		Zip:         "94402",
		Address1:    "2175 Burlingame Ave",
		PhoneNumber: "1231231234",
	}
}
