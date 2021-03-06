-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_first_text(dom, '#productTitle') as `title`,
    str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), '&node=') as `category`,
    dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as `categorylevel`,
    dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as `categorypath`,
    dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as `categorypathlevel`,
    dom_first_text(dom, '#wayfinding-breadcrumbs_container ul li:last-child a') as `categoryname`,
    dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a') as `categorynamelevel`,
    dom_first_text(dom, '#bylineInfo') as `brand`,
    dom_all_slim_htmls(dom, '#imageBlock img') as `gallery`,
    dom_first_slim_html(dom, '#imageBlock img:expr(width > 400)') as `img`,
    dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as `price`,
    str_is_not_empty(dom_first_text(dom, '#acBadge_feature_div i:contains(Best Seller):first-child')) as `isbs`,
    str_is_not_empty(dom_first_text(dom, '#acBadge_feature_div span:contains(Amazon)')) as `isac`,
    dom_all_hrefs(dom, '#buybox div:contains(Sold by):first-child a, #usedbuyBox div:contains(Sold by):first-child a, #shipsFromSoldByInsideBuyBox_feature_div #merchant-info a') as `soldby`,
    dom_all_hrefs(dom, '#buybox div:contains(Sold by):first-child a, #usedbuyBox div:contains(Sold by):first-child a, #shipsFromSoldByInsideBuyBox_feature_div #merchant-info a') as `sellerID`,
    dom_all_hrefs(dom, '#buybox div:contains(Sold by):first-child a, #usedbuyBox div:contains(Sold by):first-child a, #shipsFromSoldByInsideBuyBox_feature_div #merchant-info a') as `marketplaceID`,
    dom_first_text(dom, '#desktop_buybox #merchant-info') as `shipby`,
    str_is_not_empty(dom_first_text(dom, '#availability')) as `instock`,
    dom_all_hrefs(dom, '#availability a, #olp-upd-new-used a, #olp-upd-new a, #certified-refurbished-version a[href~=/dp/], #newer-version a[href~=/dp/]') as `sellsameurl`,
    str_substring_between(dom_first_text(dom, '#olp-upd-new-used a, #olp-upd-new a'), '(', ')') as `othersellernum`,
    str_is_not_empty(dom_first_text(dom, '#addToCart_feature_div span:contains(Add to Cart):first-child')) as `isaddcart`,
    str_is_not_empty(dom_first_text(dom, '#buyNow span:contains(Buy now):first-child')) as `isbuy`,
    dom_first_text(dom, '#productDescription, h2:contains(Product Description) + div:first-child') as `desc`,
    dom_all_slim_htmls(dom, '#prodDetails h1:contains(Feedback):first-child ~ div a') as `feedbackurl`,
    dom_first_text(dom, '#prodDetails table tr th:contains(ASIN):first-child ~ td') as `asin`,
    dom_first_text(dom, '#prodDetails table tr th:contains(Product Dimensions):first-child ~ td') as `volume`,
    dom_first_text(dom, '#prodDetails table tr th:contains(Item Weight):first-child ~ td') as `weight`,
    dom_first_text(dom, '#prodDetails table tr th:contains(Best Sellers Rank):first-child ~ td[1]') as `smallrank`,
    dom_first_text(dom, '#prodDetails table tr th:contains(Best Sellers Rank):first-child ~ td[2]') as `bigrank`,
    dom_first_text(dom, '#prodDetails table tr th:contains(Date First):first-child ~ td') as `onsaletime`,
    is_not_empty(dom_all_imgs(dom, '#prodDetails img:expr(left >= 10 && top >= 500 && width >= 400 && height >= 200)')) as `isa`,
    str_first_integer(dom_first_text(dom, 'a#askATFLink span'), 0) as `qanum`,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as `score`,
    str_substring_before(dom_first_text(dom, '#reviewsMedley div span:contains(customer ratings)'), ' customer ratings') as `starnum`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(5 star) ~ td:contains(%)') as `score5percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(4 star) ~ td:contains(%)') as `score4percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(3 star) ~ td:contains(%)') as `score3percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(2 star) ~ td:contains(%)') as `score2percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(1 star) ~ td:contains(%)') as `score1percent`,
    dom_first_href(dom, '#reviews-medley-footer a') as `reviewsurl`
from load_out_pages('https://www.amazon.com/gp/browse.html?node=16713337011&ref_=nav_em_0_2_8_5_sbdshd_cameras -i 7d -ii 30d', 'body a[href~=/dp/]');
