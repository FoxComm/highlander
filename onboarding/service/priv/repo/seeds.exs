# Script for populating the database. You can run it as:
#
#     mix run priv/repo/seeds.exs
#
# Inside the script, you can read and write to any of your
# repositories directly:
#
#     OnboardingService.Repo.insert!(%OnboardingService.SomeModel{})
#
# We recommend using the bang functions (`insert!`, `update!`
# and so on) as they will fail if something goes wrong.
alias OnboardingService.Repo
alias OnboardingService.Merchant
alias OnboardingService.MerchantApplication
alias OnboardingService.MerchantAddress
alias OnboardingService.SocialProfile
alias OnboardingService.BusinessProfile
alias OnboardingService.MerchantBusinessProfile
alias OnboardingService.MerchantSocialProfile
alias OnboardingService.MerchantApplicationBusinessProfile
alias OnboardingService.MerchantApplicationBusinessProfile

merchant_names = ~w(Merchant1 Merchant2 Merchant3)

merchant_applications = for merchant_name <- merchant_names do
  Repo.insert! %MerchantApplication{
    business_name: merchant_name,
    email_address: merchant_name <> "@donque.com",
    phone_number: "+1-123-456-7890",
    site_url: "http://site.com",
    state: "new"
  }
end
