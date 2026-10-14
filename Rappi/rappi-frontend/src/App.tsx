import { gql } from "@apollo/client";
import { useQuery } from "@apollo/client/react";

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

function App() {
  // useQuery: fire the query, get back loading/error/data
  const { loading, error, data } = useQuery<RestaurantsData>(GET_RESTAURANTS);

  if (loading) return <p>Loading restaurants...</p>;
  if (error) return <p>Error: {error.message}</p>;

  return (
    <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
      <h1>Rappi — Restaurants</h1>
      {data?.openRestaurants.map((restaurant: Restaurant) => (
        <div
          key={restaurant.id}
          style={{ border: "1px solid #ccc", borderRadius: "8px", padding: "1rem", margin: "1rem 0" }}
        >
          <h2>{restaurant.name}</h2>
          <p style={{ color: "#666" }}>{restaurant.address}</p>
          <ul>
            {restaurant.menu.map((item: MenuItem) => (
              <li key={item.id}>
                {item.name} — ${item.price}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  );
}

export default App;