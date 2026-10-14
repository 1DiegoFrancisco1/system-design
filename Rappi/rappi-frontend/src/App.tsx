import { gql } from "@apollo/client";
import { useQuery, useMutation } from "@apollo/client/react";

// The GraphQL query — the SAME one you ran in GraphiQL
const GET_RESTAURANTS = gql`
  query {
    openRestaurants {
      id
      name
      address
      menu {
        id
        name
        price
      }
    }
  }
`;

const PLACE_ORDER = gql`
  mutation PlaceOrder($input: PlaceOrderInput!) {
    placeOrder(input: $input) {
      id
      status
      total
    }
  }
`;

// TypeScript types for the data shape (your TS background pays off here)
interface MenuItem {
  id: string;
  name: string;
  price: number;
}
interface Restaurant {
  id: string;
  name: string;
  address: string;
  menu: MenuItem[];
}
interface RestaurantsData {
  openRestaurants: Restaurant[];
}
interface PlacedOrder {
  id: string;
  status: string;
  total: number;
}
interface PlaceOrderData {
  placeOrder: PlacedOrder;
}

function App() {
  // useQuery: fire the query, get back loading/error/data
  const { loading, error, data } = useQuery<RestaurantsData>(GET_RESTAURANTS);
  const [placeOrder, { data: orderData, loading: ordering }] = useMutation<PlaceOrderData>(PLACE_ORDER);

  if (loading) return <p>Loading restaurants...</p>;
  if (error) return <p>Error: {error.message}</p>;

  // Called when the user clicks "Order" on a restaurant
  const handleOrder = (restaurant: Restaurant) => {
    const firstItem = restaurant.menu[0];
    if (!firstItem) {
      alert("This restaurant has no menu items!");
      return;
    }

    placeOrder({
      variables: {
        input: {
          customerId: "550e8400-e29b-41d4-a716-446655440000",
          restaurantId: restaurant.id,
          deliveryAddress: "Condesa, CDMX",
          items: [
            {
              menuItemId: firstItem.id,
              quantity: 1,
              clientPrice: firstItem.price,
            },
          ],
          idempotencyKey: "ui-order-" + Date.now(), // unique each click
        },
      },
    });
  };

  return (
    <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
      <h1>Rappi — Restaurants</h1>

      {orderData && (
        <div style={{ background: "#e6f7e6", padding: "1rem", borderRadius: "8px", marginBottom: "1rem" }}>
          ✅ Order placed! ID: {orderData.placeOrder.id} — Status: {orderData.placeOrder.status} — Total: ${orderData.placeOrder.total}
        </div>
      )}

      {data?.openRestaurants.map((restaurant) => (
        <div
          key={restaurant.id}
          style={{ border: "1px solid #ccc", borderRadius: "8px", padding: "1rem", margin: "1rem 0" }}
        >
          <h2>{restaurant.name}</h2>
          <p style={{ color: "#666" }}>{restaurant.address}</p>
          <ul>
            {restaurant.menu.map((item) => (
              <li key={item.id}>
                {item.name} — ${item.price}
              </li>
            ))}
          </ul>
          <button onClick={() => handleOrder(restaurant)} disabled={ordering}>
            {ordering ? "Placing order..." : "Order first item"}
          </button>
        </div>
      ))}
    </div>
  );
}

export default App;