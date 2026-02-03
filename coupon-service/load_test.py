import requests
import concurrent.futures
import time

URL = "http://localhost:8080/api/coupons/request"
NUM_REQUESTS = 1000
CONCURRENT_WORKERS = 1000

def send_request(user_id):
    payload = {
        "userId": f"user-{user_id}",
        "type": "MEGADEAL"
    }
    try:
        response = requests.post(URL, json=payload)
        return response.status_code
    except requests.exceptions.RequestException as e:
        return str(e)

def main():
    print(f"Sending {NUM_REQUESTS} concurrent requests to {URL}...")
    start_time = time.time()

    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_WORKERS) as executor:
        futures = [executor.submit(send_request, i) for i in range(NUM_REQUESTS)]
        results = [future.result() for future in concurrent.futures.as_completed(futures)]

    end_time = time.time()
    print(f"Finished in {end_time - start_time:.2f} seconds.")

    status_counts = {}
    for status in results:
        status_counts[status] = status_counts.get(status, 0) + 1

    print("\nResults:")
    for status, count in status_counts.items():
        print(f"  Status {status}: {count} requests")

if __name__ == "__main__":
    main()
